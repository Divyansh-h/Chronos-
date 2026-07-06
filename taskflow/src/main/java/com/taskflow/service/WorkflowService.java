package com.taskflow.service;

import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WorkflowService {
    WorkflowResponse createWorkflow(WorkflowCreateRequest request);
    WorkflowResponse getWorkflow(UUID id);
    Page<WorkflowResponse> listWorkflows(Pageable pageable);
}
