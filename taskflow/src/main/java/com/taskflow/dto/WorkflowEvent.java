package com.taskflow.dto;

import java.util.UUID;

public record WorkflowEvent(
        String eventType,
        UUID workflowId,
        UUID taskId,
        String status
) {
}
