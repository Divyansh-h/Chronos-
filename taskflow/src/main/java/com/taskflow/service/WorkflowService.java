package com.taskflow.service;

import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;

import java.util.List;
import java.util.UUID;

public interface WorkflowService {
    WorkflowResponse createWorkflow(WorkflowCreateRequest request);
    WorkflowResponse getWorkflow(UUID id);
    List<WorkflowResponse> listWorkflows();
}
