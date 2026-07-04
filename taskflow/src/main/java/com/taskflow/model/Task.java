package com.taskflow.model;

import com.taskflow.model.enums.TaskStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id")
    private UUID workflowId;

    private String name;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private Integer retryCount;

    private Integer maxRetries;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode inputData;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode outputData;

    private String assignedWorker;

    private Instant startedAt;

    private Instant completedAt;

    private String errorLog;

    @Column(name = "dag_node_index")
    private Integer dagNodeIndex;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return id != null && id.equals(task.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
