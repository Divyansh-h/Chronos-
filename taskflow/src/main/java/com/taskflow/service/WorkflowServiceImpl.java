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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final EventPublisherService eventPublisher;
    private final DagResolutionService dagResolutionService;

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request) {
        log.info("Creating new workflow: {}", request.name());
        
        // Validate DAG structure — rejects cycles, missing refs, duplicates
        dagResolutionService.validateDag(request.dagDefinition());
        
        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDagDefinition(request.dagDefinition());
        workflow.setStatus(WorkflowStatus.PENDING);
        
        workflow = workflowRepository.save(workflow);
        
        List<Task> tasks = new ArrayList<>();
        if (request.dagDefinition().isArray()) {
            int index = 0;
            for (JsonNode node : request.dagDefinition()) {
                Task task = new Task();
                task.setWorkflowId(workflow.getId());
                task.setName(node.has("name") ? node.get("name").asText() : "Unnamed Task");
                task.setDagNodeIndex(index);
                task.setRetryCount(0);
                task.setMaxRetries(3);
                
                // Root tasks (no dependsOn) start as PENDING; tasks with deps start as BLOCKED
                boolean hasDeps = dagResolutionService.hasDependencies(request.dagDefinition(), task.getName());
                task.setStatus(hasDeps ? TaskStatus.BLOCKED : TaskStatus.PENDING);
                
                tasks.add(task);
                index++;
            }
        }
        
        tasks = taskRepository.saveAll(tasks);
        
        // Auto-transition to RUNNING so the WorkflowEngine picks it up
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflow = workflowRepository.save(workflow);
        
        log.info("Successfully created workflow {} with {} tasks ({} root, {} blocked)",
                workflow.getId(), tasks.size(),
                tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count(),
                tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count());
        
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
    public Page<WorkflowResponse> listWorkflows(Pageable pageable) {
        Page<Workflow> workflowPage = workflowRepository.findAll(pageable);
        if (workflowPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> workflowIds = workflowPage.getContent().stream().map(Workflow::getId).toList();
        List<Task> allTasks = taskRepository.findByWorkflowIdIn(workflowIds);
        
        Map<UUID, List<Task>> tasksByWorkflowId = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getWorkflowId));

        return workflowPage.map(workflow -> {
            List<Task> tasks = tasksByWorkflowId.getOrDefault(workflow.getId(), Collections.emptyList());
            return WorkflowResponse.fromEntity(workflow, tasks);
        });
    }
}
