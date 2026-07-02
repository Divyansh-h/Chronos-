package com.taskflow.repository;

import com.taskflow.model.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerNodeRepository extends JpaRepository<WorkerNode, UUID> {
    List<WorkerNode> findByLastHeartbeatAfter(Instant time);
}
