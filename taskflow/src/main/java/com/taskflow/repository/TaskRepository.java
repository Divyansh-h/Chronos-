package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByWorkflowId(UUID workflowId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByWorkflowIdAndStatus(UUID workflowId, TaskStatus status);

    /**
     * Atomically transitions a task to {@code targetStatus} only if its current
     * status is in {@code allowedStatuses}. Returns the number of rows updated
     * (0 or 1). This prevents duplicate enqueueing when multiple engine threads
     * race to claim the same PENDING task.
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :targetStatus WHERE t.id = :taskId AND t.status IN :allowedStatuses")
    int claimForEnqueue(@Param("taskId") UUID taskId,
                        @Param("targetStatus") TaskStatus targetStatus,
                        @Param("allowedStatuses") Collection<TaskStatus> allowedStatuses);
}
