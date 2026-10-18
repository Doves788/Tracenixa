-- Tracenixa Database Schema Migration V1
-- PostgreSQL & H2 compatible DDL

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'DEVELOPER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    owner_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(128) NOT NULL UNIQUE,
    created_by VARCHAR(36),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apikey_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS traces (
    id VARCHAR(36) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(128),
    root_operation_name VARCHAR(255) NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OK',
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms BIGINT NOT NULL,
    total_spans INT DEFAULT 1,
    model_name VARCHAR(128),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    estimated_cost NUMERIC(10, 6) DEFAULT 0.0,
    input_payload TEXT,
    output_payload TEXT,
    error_message TEXT,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trace_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_traces_project ON traces(project_id);
CREATE INDEX IF NOT EXISTS idx_traces_trace_id ON traces(trace_id);
CREATE INDEX IF NOT EXISTS idx_traces_start_time ON traces(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_traces_status ON traces(status);

CREATE TABLE IF NOT EXISTS spans (
    id VARCHAR(36) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    span_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64),
    project_id VARCHAR(36) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    span_kind VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    service_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OK',
    status_message TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms BIGINT NOT NULL,
    attributes TEXT,
    events TEXT,
    model_name VARCHAR(128),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    input_payload TEXT,
    output_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_span_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_spans_trace_id ON spans(trace_id);
CREATE INDEX IF NOT EXISTS idx_spans_span_id ON spans(span_id);
CREATE INDEX IF NOT EXISTS idx_spans_parent ON spans(parent_span_id);
CREATE INDEX IF NOT EXISTS idx_spans_start_time ON spans(start_time);

CREATE TABLE IF NOT EXISTS recorded_events (
    id VARCHAR(36) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    span_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    step_order INT NOT NULL,
    operation_name VARCHAR(128) NOT NULL,
    input_data TEXT,
    output_data TEXT,
    duration_ms BIGINT,
    error TEXT,
    is_mocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recorded_trace ON recorded_events(trace_id, step_order);

CREATE TABLE IF NOT EXISTS replay_sessions (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    source_trace_id VARCHAR(64) NOT NULL,
    replayed_trace_id VARCHAR(64),
    replay_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    fault_config TEXT,
    original_duration_ms BIGINT,
    replay_duration_ms BIGINT,
    steps_count INT DEFAULT 0,
    diff_summary TEXT,
    error_message TEXT,
    created_by VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_replay_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_replay_project ON replay_sessions(project_id);
CREATE INDEX IF NOT EXISTS idx_replay_status ON replay_sessions(status);

CREATE TABLE IF NOT EXISTS evaluation_datasets (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(64) NOT NULL DEFAULT 'v1.0',
    test_cases TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS evaluation_runs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    dataset_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    agent_version VARCHAR(64) NOT NULL DEFAULT 'v1.0',
    model_identifier VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_cases INT DEFAULT 0,
    passed_cases INT DEFAULT 0,
    failed_cases INT DEFAULT 0,
    average_score NUMERIC(5, 2) DEFAULT 0.0,
    faithfulness_score NUMERIC(5, 2) DEFAULT 0.0,
    relevance_score NUMERIC(5, 2) DEFAULT 0.0,
    tool_accuracy_score NUMERIC(5, 2) DEFAULT 0.0,
    detailed_results TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_evalrun_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_evalrun_dataset FOREIGN KEY (dataset_id) REFERENCES evaluation_datasets(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS regression_tests (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source_trace_id VARCHAR(64),
    input_payload TEXT NOT NULL,
    expected_tools TEXT,
    expected_assertions TEXT,
    tolerance_ms BIGINT DEFAULT 5000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_run_at TIMESTAMP WITH TIME ZONE,
    last_run_status VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_regtest_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usage_metrics (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    metric_date DATE NOT NULL,
    total_traces INT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    prompt_tokens BIGINT DEFAULT 0,
    completion_tokens BIGINT DEFAULT 0,
    estimated_cost NUMERIC(10, 4) DEFAULT 0.0,
    avg_latency_ms DOUBLE PRECISION DEFAULT 0.0,
    error_count INT DEFAULT 0,
    CONSTRAINT fk_usage_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_usage_proj_date UNIQUE (project_id, metric_date)
);
