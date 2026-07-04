package com.taskflow.engine;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkerNodeRepository;
import com.taskflow.repository.WorkflowRepository;
import com.taskflow.dto.WorkflowEvent;
import com.taskflow.model.WorkerNode;
import com.taskflow.service.EventPublisherService;
import com.taskflow.service.TaskQueueService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
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
    private final WorkflowRepository workflowRepository;
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
            Optional<Task> optionalTask = taskQueueService.pollTask(workerId);
            
            if (optionalTask.isPresent()) {
                Task task = optionalTask.get();
                log.info("Worker {} executing task {}", hostname, task.getName());
                
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
                        taskRepository.save(freshTask);
                        
                        eventPublisher.publishEvent(new WorkflowEvent("TASK_UPDATE", freshTask.getWorkflowId(), freshTask.getId(), "COMPLETED"));
                        
                        log.info("Task {} completed successfully", freshTask.getName());
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
                            
                            workflowRepository.findById(freshTask.getWorkflowId()).ifPresent(workflow -> {
                                workflow.setStatus(WorkflowStatus.FAILED);
                                workflow.setCompletedAt(Instant.now());
                                workflowRepository.save(workflow);
                                
                                eventPublisher.publishEvent(new WorkflowEvent("WORKFLOW_UPDATE", workflow.getId(), null, "FAILED"));
                                
                                log.error("Workflow {} failed due to task {}", workflow.getId(), freshTask.getId());
                            });
                        }
                    });
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
