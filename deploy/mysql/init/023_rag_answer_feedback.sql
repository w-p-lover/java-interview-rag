CREATE TABLE IF NOT EXISTS t_rag_answer_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_hash CHAR(64) NOT NULL,
    grounded TINYINT(1) NOT NULL,
    retrieval_strategy VARCHAR(32) NOT NULL,
    feedback VARCHAR(16) NOT NULL COMMENT 'HELPFUL 或 NOT_HELPFUL',
    review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、EVALUATION 或 IGNORED',
    review_note VARCHAR(500) NULL,
    failure_layer VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_rag_answer_feedback_created_at (created_at),
    KEY idx_rag_answer_feedback_feedback (feedback)
    ,KEY idx_rag_answer_feedback_review_status (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 回答低敏反馈';
