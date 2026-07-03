package com.taskflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;
import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowServiceImplTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @Test
    void createWorkflow_ShouldCreateWorkflowAndPendingTasks() throws Exception {
        // Arrange
        String jsonString = "[{\"name\": \"Task 1\"}, {\"name\": \"Task 2\"}, {\"name\": \"Task 3\"}]";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dagDefinition = mapper.readTree(jsonString);

        WorkflowCreateRequest request = new WorkflowCreateRequest("Test Workflow", dagDefinition);

        Workflow savedWorkflow = new Workflow();
        savedWorkflow.setId(UUID.randomUUID());
        savedWorkflow.setName("Test Workflow");
        savedWorkflow.setStatus(WorkflowStatus.PENDING);
        savedWorkflow.setDagDefinition(dagDefinition);

        when(workflowRepository.save(any(Workflow.class))).thenReturn(savedWorkflow);
        when(taskRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WorkflowResponse response = workflowService.createWorkflow(request);

        // Assert
        assertNotNull(response);
        assertEquals("Test Workflow", response.name());
        assertEquals(WorkflowStatus.PENDING, response.status());

        // Verify taskRepository.saveAll was called
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Task>> taskListCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository, times(1)).saveAll(taskListCaptor.capture());

        List<Task> savedTasks = taskListCaptor.getValue();
        assertEquals(3, savedTasks.size(), "Should have saved 3 tasks");

        for (Task task : savedTasks) {
            assertEquals(savedWorkflow.getId(), task.getWorkflowId(), "Task should be linked to workflow");
            assertEquals(TaskStatus.PENDING, task.getStatus(), "Task should be in PENDING state");
        }

        verify(eventPublisher, times(1)).publishEvent(any(com.taskflow.dto.WorkflowEvent.class));
    }
}
