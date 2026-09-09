CREATE TABLE IF NOT EXISTS t_knowledge_review_task (
    id CHAR(64) NOT NULL COMMENT '问题和证据断言的确定性 SHA-256 ID',
    question TEXT NOT NULL,
    claim TEXT NOT NULL,
    evidence_status VARCHAR(32) NOT NULL,
    source_numbers_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_review_date DATE NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_knowledge_review_status_date (status, planned_review_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识问答证据问题复习任务';
