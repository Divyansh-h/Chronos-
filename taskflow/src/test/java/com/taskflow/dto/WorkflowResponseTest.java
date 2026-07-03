package com.taskflow.dto;

import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowResponseTest {

    @Test
    void fromEntity_ShouldMapCorrectly() {
        // Arrange
        UUID workflowId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant completedAt = Instant.now();

        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setName("Test Workflow");
        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow.setCreatedAt(createdAt);
        workflow.setCompletedAt(completedAt);

        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setWorkflowId(workflowId);
        task.setName("Test Task");
        task.setStatus(TaskStatus.COMPLETED);
        task.setRetryCount(1);
        task.setAssignedWorker("worker-1");
        task.setStartedAt(createdAt);
        task.setCompletedAt(completedAt);
        
        List<Task> tasks = Collections.singletonList(task);

        // Act
        WorkflowResponse response = WorkflowResponse.fromEntity(workflow, tasks);

        // Assert
        assertNotNull(response);
        assertEquals(workflowId, response.id());
        assertEquals("Test Workflow", response.name());
        assertEquals(WorkflowStatus.COMPLETED, response.status());
        assertEquals(createdAt, response.createdAt());
        assertEquals(completedAt, response.completedAt());

        assertNotNull(response.tasks());
        assertEquals(1, response.tasks().size());

        TaskResponse taskResponse = response.tasks().get(0);
        assertEquals(taskId, taskResponse.id());
        assertEquals("Test Task", taskResponse.name());
        assertEquals(TaskStatus.COMPLETED, taskResponse.status());
        assertEquals(1, taskResponse.retryCount());
        assertEquals("worker-1", taskResponse.assignedWorker());
        assertEquals(createdAt, taskResponse.startedAt());
        assertEquals(completedAt, taskResponse.completedAt());
    }

    @Test
    void fromEntity_ShouldHandleNullTasksListSafely() {
        // Arrange
        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setName("No Tasks Workflow");
        workflow.setStatus(WorkflowStatus.PENDING);

        // Act
        WorkflowResponse response = WorkflowResponse.fromEntity(workflow, null);

        // Assert
        assertNotNull(response);
        assertEquals("No Tasks Workflow", response.name());
        assertNotNull(response.tasks());
        assertTrue(response.tasks().isEmpty(), "Tasks list should be empty, not null");
    }

    @Test
    void fromEntity_ShouldReturnNullWhenWorkflowIsNull() {
        // Act
        WorkflowResponse response = WorkflowResponse.fromEntity(null, Collections.emptyList());

        // Assert
        assertNull(response, "Response should be null when workflow is null");
    }
}
