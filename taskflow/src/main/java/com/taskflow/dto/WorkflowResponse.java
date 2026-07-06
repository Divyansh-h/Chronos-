package com.taskflow.dto;

import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.WorkflowStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record WorkflowResponse(
        UUID id,
        String name,
        WorkflowStatus status,
        Instant createdAt,
        Instant completedAt,
        JsonNode dagDefinition,
        List<TaskResponse> tasks
) {
    public static WorkflowResponse fromEntity(Workflow workflow, List<Task> tasks) {
        if (workflow == null) {
            return null;
        }

        List<TaskResponse> taskResponses = tasks == null 
                ? Collections.emptyList() 
                : tasks.stream()
                       .map(TaskResponse::fromEntity)
                       .collect(Collectors.toList());

        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getStatus(),
                workflow.getCreatedAt(),
                workflow.getCompletedAt(),
                workflow.getDagDefinition(),
                taskResponses
        );
    }
}
