package com.taskflow.controller;

import com.taskflow.dto.TaskPollRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.model.Task;
import com.taskflow.service.TaskQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskQueueService taskQueueService;

    @PostMapping("/poll")
    public ResponseEntity<TaskResponse> pollTask(@Valid @RequestBody TaskPollRequest request) {
        Optional<Task> taskOpt = taskQueueService.pollTask(request.workerId());
        
        return taskOpt.map(task -> ResponseEntity.ok(TaskResponse.fromEntity(task)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
