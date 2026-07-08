package com.taskflow.service;

import com.taskflow.model.Task;

import java.util.Optional;

public interface TaskQueueService {
    void enqueueTask(Task task);
    Optional<Task> pollTask(String workerId);
    long getQueueSize();
}
