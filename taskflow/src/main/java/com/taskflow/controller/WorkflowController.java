package com.taskflow.controller;

import com.taskflow.dto.WorkflowCreateRequest;
import com.taskflow.dto.WorkflowResponse;
import com.taskflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody WorkflowCreateRequest request) {
        WorkflowResponse response = workflowService.createWorkflow(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable UUID id) {
        WorkflowResponse response = workflowService.getWorkflow(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<WorkflowResponse>> listWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WorkflowResponse> response = workflowService.listWorkflows(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelWorkflow(@PathVariable UUID id) {
        workflowService.cancelWorkflow(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryWorkflow(@PathVariable UUID id) {
        workflowService.retryWorkflow(id);
        return ResponseEntity.ok().build();
    }
}
