package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record TaskPollRequest(
    @NotBlank(message = "workerId is required")
    String workerId,
    
    @Min(value = 1, message = "maxTasks must be at least 1")
    Integer maxTasks
) {
}
