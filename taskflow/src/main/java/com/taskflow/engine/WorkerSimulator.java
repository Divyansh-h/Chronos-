package com.taskflow.engine;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkerNodeRepository;
import com.taskflow.dto.WorkflowEvent;
import com.taskflow.model.WorkerNode;
import com.taskflow.service.EventPublisherService;
import com.taskflow.service.TaskQueueService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WorkerSimulator {

    private final WorkerNodeRepository workerNodeRepository;
    private final TaskQueueService taskQueueService;
    private final TaskRepository taskRepository;
    private final EventPublisherService eventPublisher;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void initWorkers() {
        for (int i = 1; i <= 3; i++) {
            WorkerNode worker = new WorkerNode();
            worker.setHostname("worker-" + i);
            worker.setStatus("ONLINE");
            worker.setTasksCompleted(0);
            worker.setCurrentLoad(0);
            worker.setLastHeartbeat(Instant.now());
            
            workerNodeRepository.save(worker);
            log.info("Initialized simulated worker: {}", worker.getHostname());
            
            executorService.submit(() -> startWorkerLoop(worker.getId().toString(), worker.getHostname()));
        }
    }

    private void startWorkerLoop(String workerId, String hostname) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Optional<Task> optionalTask = taskQueueService.pollTask(workerId);
                
                // Periodically update heartbeat
                workerNodeRepository.findById(UUID.fromString(workerId)).ifPresent(w -> {
                    w.setLastHeartbeat(Instant.now());
                    workerNodeRepository.save(w);
                });
                
                if (optionalTask.isPresent()) {
                    Task task = optionalTask.get();
                    try {
                        MDC.put("workerId", workerId);
                        MDC.put("taskId", task.getId().toString());
                        if (task.getWorkflowId() != null) {
                            MDC.put("workflowId", task.getWorkflowId().toString());
                        }
                        
                        log.info("Worker {} executing task {} with inputs: {}", hostname, task.getName(), task.getInputData());
                        
                        // Increment currentLoad
                        workerNodeRepository.findById(UUID.fromString(workerId)).ifPresent(w -> {
                            w.setCurrentLoad(w.getCurrentLoad() + 1);
                            workerNodeRepository.save(w);
                        });
                        
                        try {
                            Thread.sleep((long) (Math.random() * 3000 + 1000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        boolean success = Math.random() < 0.9;
                        if (success) {
                            taskRepository.findById(task.getId()).ifPresent(freshTask -> {
                                freshTask.setStatus(TaskStatus.COMPLETED);
                                freshTask.setCompletedAt(Instant.now());
                                
                                com.fasterxml.jackson.databind.node.ObjectNode outputNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                                outputNode.put("status", "processed");
                                outputNode.put("records_handled", (int)(Math.random() * 1000));
                                outputNode.put("worker_id", workerId);
                                freshTask.setOutputData(outputNode);
                                
                                taskRepository.save(freshTask);
                                
                                eventPublisher.publishEvent(new WorkflowEvent("TASK_UPDATE", freshTask.getWorkflowId(), freshTask.getId(), "COMPLETED"));
                                
                                log.info("Task {} completed successfully", freshTask.getName());
                            });
                            
                            // Decrement currentLoad, increment tasksCompleted
                            workerNodeRepository.findById(UUID.fromString(workerId)).ifPresent(w -> {
                                w.setCurrentLoad(Math.max(0, w.getCurrentLoad() - 1));
                                w.setTasksCompleted(w.getTasksCompleted() + 1);
                                workerNodeRepository.save(w);
                            });
                        } else {
                            taskRepository.findById(task.getId()).ifPresent(freshTask -> {
                                int currentRetries = freshTask.getRetryCount() == null ? 0 : freshTask.getRetryCount();
                                int maxRetries = freshTask.getMaxRetries() == null ? 3 : freshTask.getMaxRetries();
                                freshTask.setRetryCount(currentRetries + 1);
                                
                                if (freshTask.getRetryCount() <= maxRetries) {
                                    freshTask.setStatus(TaskStatus.PENDING);
                                    freshTask.setErrorLog("Random simulated failure");
                                    taskRepository.save(freshTask);
                                    
                                    eventPublisher.publishEvent(new WorkflowEvent("TASK_UPDATE", freshTask.getWorkflowId(), freshTask.getId(), "PENDING"));
                                    
                                    log.warn("Task {} failed, retrying ({}/{})", freshTask.getName(), freshTask.getRetryCount(), maxRetries);
                                } else {
                                    freshTask.setStatus(TaskStatus.FAILED);
                                    freshTask.setErrorLog("Random simulated failure - Max retries exceeded");
                                    taskRepository.save(freshTask);
                                    
                                    eventPublisher.publishEvent(new WorkflowEvent("TASK_UPDATE", freshTask.getWorkflowId(), freshTask.getId(), "FAILED"));
                                    
                                    log.warn("Task {} permanently failed after {}/{} retries. WorkflowEngine will evaluate workflow state.",
                                            freshTask.getName(), freshTask.getRetryCount(), maxRetries);
                                }
                            });
                            
                            // Decrement currentLoad
                            workerNodeRepository.findById(UUID.fromString(workerId)).ifPresent(w -> {
                                w.setCurrentLoad(Math.max(0, w.getCurrentLoad() - 1));
                                workerNodeRepository.save(w);
                            });
                        }
                    } finally {
                        MDC.remove("workerId");
                        MDC.remove("taskId");
                        MDC.remove("workflowId");
                    }
                }
            } catch (Throwable t) {
                log.error("Worker {} encountered a fatal error during execution loop!", hostname, t);
                try {
                    Thread.sleep(2000); // Backoff before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down worker simulator...");
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Worker threads did not terminate within 5 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Worker simulator shut down complete.");
    }
}
