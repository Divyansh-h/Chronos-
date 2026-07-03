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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    void enqueueTask_ShouldSaveToDbAndPushToRedis() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        Task mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setStatus(TaskStatus.PENDING);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);

        // Act
        taskQueueService.enqueueTask(taskId);

        // Assert
        assertEquals(TaskStatus.QUEUED, mockTask.getStatus());
        verify(taskRepository, times(1)).save(mockTask);
        verify(listOperations, times(1)).leftPush("taskflow:queue:pending", taskId.toString());
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
