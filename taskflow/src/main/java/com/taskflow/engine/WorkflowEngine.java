package com.taskflow.engine;

import com.taskflow.repository.TaskRepository;
import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.WorkflowRepository;
import com.taskflow.service.TaskQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final TaskQueueService taskQueueService;

    @Scheduled(fixedDelayString = "${taskflow.engine.polling-interval-ms}")
    public void processWorkflows() {
        List<Workflow> runningWorkflows = workflowRepository.findByStatus(WorkflowStatus.RUNNING);
        
        for (Workflow workflow : runningWorkflows) {
            log.info("Evaluating workflow: {}", workflow.getId());
            
            List<Task> tasks = taskRepository.findByWorkflowId(workflow.getId());
            List<Task> pendingTasks = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING)
                    .toList();
                    
            for (Task task : pendingTasks) {
                try {
                    taskQueueService.enqueueTask(task.getId());
                } catch (Exception e) {
                    log.warn("Failed to enqueue task {} for workflow {}: {}", task.getId(), workflow.getId(), e.getMessage());
                }
            }
        }
    }
}
