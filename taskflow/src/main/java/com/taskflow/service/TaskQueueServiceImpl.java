package com.taskflow.service;

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
public class TaskQueueServiceImpl implements TaskQueueService {

    private static final String QUEUE_KEY = "taskflow:queue:pending";
    private static final long POLL_TIMEOUT_SECONDS = 2;

    private final StringRedisTemplate stringRedisTemplate;
    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public void enqueueTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.FAILED) {
            throw new IllegalArgumentException("Task must be PENDING or FAILED to be enqueued");
        }

        task.setStatus(TaskStatus.QUEUED);
        taskRepository.save(task);

        try {
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, taskId.toString());
            log.info("Task enqueued: {}", taskId);
        } catch (RedisConnectionFailureException e) {
            log.error("Failed to enqueue task to Redis. Rolling back transaction for task: {}", taskId, e);
            throw new RuntimeException("Redis connection failed, rolling back task enqueue", e);
        }
    }

    @Override
    public Optional<Task> pollTask(String workerId) {
        try {
            String taskIdStr = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY, Duration.ofSeconds(POLL_TIMEOUT_SECONDS));
            if (taskIdStr == null) {
                return Optional.empty();
            }
            
            UUID taskId = UUID.fromString(taskIdStr);
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setStatus(TaskStatus.RUNNING);
                task.setAssignedWorker(workerId);
                task.setStartedAt(Instant.now());
                
                task = taskRepository.save(task);
                log.info("Worker {} claimed task {}", workerId, taskId);
                return Optional.of(task);
            } else {
                log.warn("Task ID found in Redis but missing in DB: {}", taskId);
                return Optional.empty();
            }
            
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to poll task queue: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error polling task from Redis for worker: {}", workerId, e);
            return Optional.empty();
        }
    }

    @Override
    public long getQueueSize() {
        Long size = stringRedisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }
}
