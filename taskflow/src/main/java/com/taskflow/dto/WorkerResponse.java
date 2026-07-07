package com.taskflow.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkerResponse(
        UUID id,
        String hostname,
        String status,
        Instant lastHeartbeat,
        Integer tasksCompleted,
        Integer currentLoad
) {}
