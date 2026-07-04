package com.taskflow.service;

import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowEvent;
import com.taskflow.dto.WorkflowResponse;
import com.taskflow.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final EventPublisherService eventPublisher;

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request) {
        log.info("Creating new workflow: {}", request.name());
        
        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDagDefinition(request.dagDefinition());
        workflow.setStatus(WorkflowStatus.PENDING);
        
        workflow = workflowRepository.save(workflow);
        
        List<Task> tasks = new ArrayList<>();
        if (request.dagDefinition().isArray()) {
            for (JsonNode node : request.dagDefinition()) {
                Task task = new Task();
                task.setWorkflowId(workflow.getId());
                task.setName(node.has("name") ? node.get("name").asText() : "Unnamed Task");
                task.setStatus(TaskStatus.PENDING);
                task.setRetryCount(0);
                task.setMaxRetries(3);
                tasks.add(task);
            }
        }
        
        tasks = taskRepository.saveAll(tasks);
        
        // Auto-transition to RUNNING so the WorkflowEngine picks it up
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflow = workflowRepository.save(workflow);
        
        log.info("Successfully created workflow {} with {} tasks", workflow.getId(), tasks.size());
        
        eventPublisher.publishEvent(new WorkflowEvent("WORKFLOW_UPDATE", workflow.getId(), null, workflow.getStatus().name()));
        
        return WorkflowResponse.fromEntity(workflow, tasks);
    }

    @Override
    public WorkflowResponse getWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        
        List<Task> tasks = taskRepository.findByWorkflowId(id);
        
        return WorkflowResponse.fromEntity(workflow, tasks);
    }

    @Override
    public List<WorkflowResponse> listWorkflows() {
        // To be implemented
        return null;
    }
}
