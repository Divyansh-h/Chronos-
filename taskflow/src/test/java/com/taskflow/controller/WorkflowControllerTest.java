package com.taskflow.controller;

import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
public class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowService workflowService;

    @Test
    void createWorkflow_ShouldReturn201AndWorkflowResponse() throws Exception {
        // Arrange
        UUID workflowId = UUID.randomUUID();
        String jsonPayload = """
                {
                  "name": "Data Processing Pipeline",
                  "dagDefinition": [
                    { "name": "Extract" },
                    { "name": "Transform" },
                    { "name": "Load" }
                  ]
                }
                """;

        WorkflowResponse mockResponse = new WorkflowResponse(
                workflowId,
                "Data Processing Pipeline",
                WorkflowStatus.PENDING,
                Instant.now(),
                null,
                Collections.emptyList()
        );

        when(workflowService.createWorkflow(any(WorkflowCreateRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workflowId.toString()))
                .andExpect(jsonPath("$.name").value("Data Processing Pipeline"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
