package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByWorkflowId(UUID workflowId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByWorkflowIdAndStatus(UUID workflowId, TaskStatus status);
}
