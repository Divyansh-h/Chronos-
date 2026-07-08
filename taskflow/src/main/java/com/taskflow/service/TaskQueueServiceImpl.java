package com.taskflow.service;

import com.taskflow.dto.WorkflowEvent;
import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TaskQueueServiceImpl implements TaskQueueService {

    private static final String QUEUE_KEY = "taskflow:queue:pending";
    private static final long POLL_TIMEOUT_SECONDS = 2;

    private final StringRedisTemplate stringRedisTemplate;
    private final TaskRepository taskRepository;
    private final EventPublisherService eventPublisher;

    @Override
    public void enqueueTask(Task task) {
        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.FAILED) {
            log.debug("Task {} not in enqueueable state, skipping", task.getId());
            return;
        }

        task.setStatus(TaskStatus.QUEUED);
        task = taskRepository.save(task);
        
        final String taskId = task.getId().toString();
        
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, taskId);
                            log.info("Task enqueued: {}", taskId);
                        } catch (Exception e) {
                            log.error("Failed to enqueue task to Redis after commit: {}", taskId, e);
                        }
                    }
                }
            );
        } else {
            try {
                stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, taskId);
                log.info("Task enqueued (no active tx): {}", taskId);
            } catch (Exception e) {
                log.error("Failed to enqueue task to Redis: {}", taskId, e);
            }
        }
    }

    @Override
    @Transactional
    public Optional<Task> pollTask(String workerId) {
        String taskIdStr;
        try {
            taskIdStr = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY, Duration.ofSeconds(POLL_TIMEOUT_SECONDS));
            if (taskIdStr == null) {
                return Optional.empty();
            }
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to poll task queue: {}", e.getMessage());
            return Optional.empty();
        }

        UUID taskId = UUID.fromString(taskIdStr);

        try {
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                
                // Prevent duplicate execution if task is already in a terminal/running state
                if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.RUNNING) {
                    log.warn("Popped task {} from queue, but it is already in state {}. Discarding.", taskId, task.getStatus());
                    return Optional.empty();
                }

                task.setStatus(TaskStatus.RUNNING);
                task.setAssignedWorker(workerId);
                task.setStartedAt(Instant.now());

                task = taskRepository.save(task);

                eventPublisher.publishEvent(new WorkflowEvent("TASK_UPDATE", task.getWorkflowId(), task.getId(), task.getStatus().name()));

                log.info("Worker {} claimed task {}", workerId, taskId);
                return Optional.of(task);
            } else {
                log.warn("Task ID found in Redis but missing in DB: {}", taskId);
                return Optional.empty();
            }
        } catch (Exception e) {
            // Compensating action: re-push the task back to Redis so it isn't permanently lost
            log.error("Failed to claim task {} for worker {}. Re-pushing to queue.", taskId, workerId, e);
            try {
                stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, taskIdStr);
                log.info("Task {} re-pushed to queue after failed claim", taskId);
            } catch (Exception requeueEx) {
                log.error("CRITICAL: Failed to re-push task {} to Redis. Task may be orphaned.", taskId, requeueEx);
            }
            return Optional.empty();
        }
    }

    @Override
    public long getQueueSize() {
        Long size = stringRedisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }
}
