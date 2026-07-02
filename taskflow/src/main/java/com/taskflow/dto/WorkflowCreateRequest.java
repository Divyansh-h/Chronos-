package com.taskflow.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowCreateRequest(
        @NotBlank
        String name,
        
        @NotNull
        JsonNode dagDefinition
) {
}
