package com.taskflow.integration;

import com.taskflow.model.WorkerNode;
import com.taskflow.repository.WorkerNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class WorkerNodeRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private WorkerNodeRepository workerNodeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findByLastHeartbeatAfter_ShouldReturnOnlyActiveNodes() {
        // Arrange: Clear any existing test data
        workerNodeRepository.deleteAll();

        // Node 1: Active (Heartbeat now)
        WorkerNode node1 = new WorkerNode();
        node1.setHostname("active-worker");
        node1.setStatus("ACTIVE");
        node1 = workerNodeRepository.save(node1);

        // Node 2: Stale (Heartbeat 5 minutes ago)
        WorkerNode node2 = new WorkerNode();
        node2.setHostname("stale-worker");
        node2.setStatus("ACTIVE");
        node2 = workerNodeRepository.save(node2);

        // Node 3: Dead (Heartbeat 15 minutes ago)
        WorkerNode node3 = new WorkerNode();
        node3.setHostname("dead-worker");
        node3.setStatus("INACTIVE");
        node3 = workerNodeRepository.save(node3);

        // Bypass Hibernate's @UpdateTimestamp annotation by modifying the database directly using JdbcTemplate
        jdbcTemplate.update("UPDATE worker_nodes SET last_heartbeat = ? WHERE id = ?", 
                Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)), node2.getId());
                
        jdbcTemplate.update("UPDATE worker_nodes SET last_heartbeat = ? WHERE id = ?", 
                Timestamp.from(Instant.now().minus(15, ChronoUnit.MINUTES)), node3.getId());

        // Act: Find nodes that have a heartbeat within the last 2 minutes
        Instant threshold = Instant.now().minus(2, ChronoUnit.MINUTES);
        List<WorkerNode> activeNodes = workerNodeRepository.findByLastHeartbeatAfter(threshold);

        // Assert
        assertEquals(1, activeNodes.size(), "Should only return the active worker");
        assertEquals("active-worker", activeNodes.get(0).getHostname());
        
        // Ensure the others are correctly excluded
        boolean containsStaleOrDead = activeNodes.stream()
                .anyMatch(n -> n.getHostname().equals("stale-worker") || n.getHostname().equals("dead-worker"));
        assertFalse(containsStaleOrDead, "Should not contain stale or dead workers");
    }
}
