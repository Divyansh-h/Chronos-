package com.taskflow.dto;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record TaskResponse(
        UUID id,
        String name,
        TaskStatus status,
        Integer retryCount,
        JsonNode inputData,
        JsonNode outputData,
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
                task.getInputData(),
                task.getOutputData(),
                task.getAssignedWorker(),
                task.getStartedAt(),
                task.getCompletedAt()
        );
    }
}
