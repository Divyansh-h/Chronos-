package com.taskflow.service;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class TaskQueueServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private TaskQueueServiceImpl taskQueueService;

    @Test
    void enqueueTask_ShouldClaimAtomicallyAndPushToRedis() {
        // Arrange
        UUID taskId = UUID.randomUUID();

        // Simulate a successful atomic claim (1 row updated)
        when(taskRepository.claimForEnqueue(
                eq(taskId), eq(TaskStatus.QUEUED), eq(List.of(TaskStatus.PENDING, TaskStatus.FAILED))))
                .thenReturn(1);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);

        // Act
        taskQueueService.enqueueTask(taskId);

        // Assert: atomic claim was invoked, and task was pushed to Redis
        verify(taskRepository, times(1)).claimForEnqueue(
                eq(taskId), eq(TaskStatus.QUEUED), eq(List.of(TaskStatus.PENDING, TaskStatus.FAILED)));
        verify(listOperations, times(1)).leftPush("taskflow:queue:pending", taskId.toString());
        // No findById or save should be called — the atomic UPDATE handles the state transition
        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void enqueueTask_ShouldSkipWhenAlreadyClaimed() {
        // Arrange: simulate another thread already claimed this task (0 rows updated)
        UUID taskId = UUID.randomUUID();

        when(taskRepository.claimForEnqueue(
                eq(taskId), eq(TaskStatus.QUEUED), eq(List.of(TaskStatus.PENDING, TaskStatus.FAILED))))
                .thenReturn(0);

        // Act
        taskQueueService.enqueueTask(taskId);

        // Assert: Redis should NEVER be touched — the task was already claimed
        verify(stringRedisTemplate, never()).opsForList();
    }

    @Test
    void pollTask_ShouldPopFromRedisAndSetToRunning() {
        // Arrange
        String workerId = "test-worker-1";
        UUID taskId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        
        Task mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setWorkflowId(workflowId);
        mockTask.setStatus(TaskStatus.QUEUED);

        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq("taskflow:queue:pending"), any(java.time.Duration.class)))
                .thenReturn(taskId.toString());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Optional<Task> result = taskQueueService.pollTask(workerId);

        // Assert
        assertTrue(result.isPresent(), "Task should be returned");
        Task polledTask = result.get();
        assertEquals(TaskStatus.RUNNING, polledTask.getStatus());
        assertEquals(workerId, polledTask.getAssignedWorker());
        assertNotNull(polledTask.getStartedAt());
        
        verify(taskRepository, times(1)).save(mockTask);
        verify(eventPublisher, times(1)).publishEvent(any(com.taskflow.dto.WorkflowEvent.class));
    }
}
