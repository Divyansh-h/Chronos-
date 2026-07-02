package com.taskflow.service;

import com.taskflow.model.Task;

import java.util.Optional;
import java.util.UUID;

public interface TaskQueueService {
    void enqueueTask(UUID taskId);
    Optional<Task> pollTask(String workerId);
    long getQueueSize();
}
