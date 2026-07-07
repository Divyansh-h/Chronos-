package com.taskflow.controller;

import com.taskflow.dto.WorkerResponse;
import com.taskflow.repository.WorkerNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerNodeRepository workerNodeRepository;

    @GetMapping
    public ResponseEntity<Page<WorkerResponse>> listWorkers(@org.springframework.lang.NonNull Pageable pageable) {
        Page<WorkerResponse> workers = workerNodeRepository.findAll(pageable)
                .map(worker -> new WorkerResponse(
                        worker.getId(),
                        worker.getHostname(),
                        worker.getStatus(),
                        worker.getLastHeartbeat(),
                        worker.getTasksCompleted(),
                        worker.getCurrentLoad()
                ));
        return ResponseEntity.ok(workers);
    }
}
