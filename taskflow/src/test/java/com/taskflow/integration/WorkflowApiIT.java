package com.taskflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorkflowApiIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndFetchWorkflowEndToEnd() {
        // Arrange
        ArrayNode dagDefinition = objectMapper.createArrayNode();
        dagDefinition.addObject().put("name", "Integration Task 1");
        dagDefinition.addObject().put("name", "Integration Task 2");
        
        WorkflowCreateRequest request = new WorkflowCreateRequest("End-to-End Workflow", dagDefinition);

        // Act 1: HTTP POST to create workflow
        ResponseEntity<WorkflowResponse> postResponse = restTemplate.postForEntity(
                "/api/v1/workflows", request, WorkflowResponse.class);

        // Assert 1: Creation Success
        assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
        WorkflowResponse postBody = postResponse.getBody();
        assertNotNull(postBody);
        assertEquals("End-to-End Workflow", postBody.name());
        assertNotNull(postBody.id());

        // Act 2: HTTP GET to fetch workflow
        ResponseEntity<WorkflowResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/workflows/" + postBody.id(), WorkflowResponse.class);

        // Assert 2: Fetch Success and Data Integrity
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        WorkflowResponse getBody = getResponse.getBody();
        assertNotNull(getBody);
        assertEquals(postBody.id(), getBody.id());
        assertEquals("End-to-End Workflow", getBody.name());
        
        assertNotNull(getBody.tasks());
        assertEquals(2, getBody.tasks().size(), "Should have exactly 2 child tasks mapped and returned");
    }
}
