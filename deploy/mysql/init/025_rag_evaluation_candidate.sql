CREATE TABLE IF NOT EXISTS t_rag_evaluation_candidate (
    id CHAR(64) NOT NULL,
    feedback_id BIGINT NULL,
    question VARCHAR(2000) NOT NULL,
    expected_sources_json JSON NOT NULL,
    expected_answer_points_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、ACTIVE 或 IGNORED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_rag_evaluation_candidate_status_created (status, created_at),
    KEY idx_rag_evaluation_candidate_feedback (feedback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工确认的 RAG 回归评估候选';
