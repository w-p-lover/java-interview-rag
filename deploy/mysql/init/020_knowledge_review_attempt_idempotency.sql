ALTER TABLE t_knowledge_review_attempt
    ADD COLUMN idempotency_key VARCHAR(128) NULL AFTER task_id,
    ADD UNIQUE KEY uk_knowledge_review_attempt_idempotency (idempotency_key);
