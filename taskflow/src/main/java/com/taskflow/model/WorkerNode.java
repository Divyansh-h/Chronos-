package com.taskflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker_nodes")
@Getter
@Setter
@NoArgsConstructor
public class WorkerNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String hostname;

    private String status;

    @UpdateTimestamp
    private Instant lastHeartbeat;

    private Integer tasksCompleted = 0;

    private Integer currentLoad = 0;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerNode)) return false;
        WorkerNode that = (WorkerNode) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
