CREATE TABLE IF NOT EXISTS t_knowledge_document_task (
    task_id CHAR(36) NOT NULL,
    attempt_id CHAR(36) NOT NULL COMMENT '当前执行尝试 ID，用于拒绝旧 Worker 迟到写入',
    source VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    operation VARCHAR(16) NOT NULL COMMENT 'IMPORT 或 REBUILD',
    status VARCHAR(16) NOT NULL COMMENT 'QUEUED、RUNNING、SUCCEEDED、FAILED 或 CANCELLED',
    version CHAR(64) NULL,
    imported TINYINT(1) NOT NULL DEFAULT 0,
    chunk_count INT UNSIGNED NOT NULL DEFAULT 0,
    format VARCHAR(16) NULL,
    warning_count INT UNSIGNED NOT NULL DEFAULT 0,
    error_type VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (task_id),
    KEY idx_knowledge_document_task_status (status),
    KEY idx_knowledge_document_task_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识资料异步任务';
