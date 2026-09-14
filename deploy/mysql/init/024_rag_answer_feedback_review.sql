ALTER TABLE t_rag_answer_feedback
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、EVALUATION 或 IGNORED',
    ADD COLUMN IF NOT EXISTS review_note VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS failure_layer VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at DATETIME(6) NULL,
    ADD KEY IF NOT EXISTS idx_rag_answer_feedback_review_status (review_status);
