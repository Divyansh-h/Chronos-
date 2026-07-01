CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    dag_definition JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    input_data JSONB,
    output_data JSONB,
    assigned_worker VARCHAR(255),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_log TEXT,
    CONSTRAINT fk_workflow
      FOREIGN KEY(workflow_id) 
      REFERENCES workflows(id)
      ON DELETE CASCADE
);

CREATE TABLE worker_nodes (
    id UUID PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_heartbeat TIMESTAMP NOT NULL,
    tasks_completed INT DEFAULT 0,
    current_load INT DEFAULT 0
);

CREATE INDEX idx_tasks_workflow_id ON tasks(workflow_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_workflows_status ON workflows(status);
