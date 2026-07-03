package com.taskflow.engine;

import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkflowRepository;
import com.taskflow.service.TaskQueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowEngineTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskQueueService taskQueueService;

    @InjectMocks
    private WorkflowEngine workflowEngine;

    @Test
    void processWorkflows_ShouldEnqueuePendingTasks() {
        // Arrange
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setStatus(WorkflowStatus.RUNNING);

        UUID pendingTaskId = UUID.randomUUID();
        Task pendingTask = new Task();
        pendingTask.setId(pendingTaskId);
        pendingTask.setWorkflowId(workflowId);
        pendingTask.setStatus(TaskStatus.PENDING);

        UUID completedTaskId = UUID.randomUUID();
        Task completedTask = new Task();
        completedTask.setId(completedTaskId);
        completedTask.setWorkflowId(workflowId);
        completedTask.setStatus(TaskStatus.COMPLETED);

        when(workflowRepository.findByStatus(WorkflowStatus.RUNNING))
                .thenReturn(Collections.singletonList(workflow));
        when(taskRepository.findByWorkflowId(workflowId))
                .thenReturn(Arrays.asList(pendingTask, completedTask));

        // Act
        workflowEngine.processWorkflows();

        // Assert
        verify(taskQueueService, times(1)).enqueueTask(pendingTaskId);
        verify(taskQueueService, never()).enqueueTask(completedTaskId);
    }
}
