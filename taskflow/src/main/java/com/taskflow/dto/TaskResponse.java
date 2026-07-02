package com.taskflow.dto;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String name,
        TaskStatus status,
        Integer retryCount,
        String assignedWorker,
        Instant startedAt,
        Instant completedAt
) {
    public static TaskResponse fromEntity(Task task) {
        if (task == null) {
            return null;
        }
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getStatus(),
                task.getRetryCount(),
                task.getAssignedWorker(),
                task.getStartedAt(),
                task.getCompletedAt()
        );
    }
}
