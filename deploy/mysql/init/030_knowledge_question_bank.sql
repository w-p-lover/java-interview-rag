CREATE TABLE IF NOT EXISTS t_knowledge_bank_question (
    id VARCHAR(36) NOT NULL,
    generation_task_id VARCHAR(36) NULL,
    source VARCHAR(500) NOT NULL,
    document_version VARCHAR(100) NOT NULL,
    content_key CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'DRAFT/ACTIVE/ARCHIVED/STALE',
    question_json MEDIUMTEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    use_count INT NOT NULL DEFAULT 0,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_question_content (content_key),
    KEY idx_bank_question_source (source(150), updated_at),
    KEY idx_bank_question_generation (generation_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS t_knowledge_bank_generation_task (
    id VARCHAR(36) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    active_key CHAR(64) NULL COMMENT '同资料当前版本的活动任务唯一键，终态为空',
    source VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'QUEUED/PROCESSING/COMPLETED/FAILED',
    task_json MEDIUMTEXT NOT NULL,
    attempt_id VARCHAR(36) NULL COMMENT '当前执行 attempt ID',
    worker_id VARCHAR(100) NULL COMMENT '当前执行 Worker ID',
    started_at DATETIME(6) NULL COMMENT '当前 attempt 开始时间',
    last_heartbeat_at DATETIME(6) NULL COMMENT '当前 attempt 最后心跳时间',
    recovery_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失联自动恢复次数',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_generation_request (request_id),
    UNIQUE KEY uk_bank_generation_active (active_key),
    KEY idx_bank_generation_source (source(150), created_at),
    KEY idx_bank_generation_lease (status, last_heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS t_knowledge_bank_question_usage (
    session_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    question_index INT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (session_id, question_id),
    KEY idx_bank_usage_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
