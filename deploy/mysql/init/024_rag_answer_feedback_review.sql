SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_rag_answer_feedback'
          AND COLUMN_NAME = 'review_status'
    ),
    'SELECT 1',
    'ALTER TABLE t_rag_answer_feedback ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING、EVALUATION 或 IGNORED'''
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_rag_answer_feedback'
          AND COLUMN_NAME = 'review_note'
    ),
    'SELECT 1',
    'ALTER TABLE t_rag_answer_feedback ADD COLUMN review_note VARCHAR(500) NULL'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_rag_answer_feedback'
          AND COLUMN_NAME = 'failure_layer'
    ),
    'SELECT 1',
    'ALTER TABLE t_rag_answer_feedback ADD COLUMN failure_layer VARCHAR(32) NULL'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_rag_answer_feedback'
          AND COLUMN_NAME = 'reviewed_at'
    ),
    'SELECT 1',
    'ALTER TABLE t_rag_answer_feedback ADD COLUMN reviewed_at DATETIME(6) NULL'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_rag_answer_feedback'
          AND INDEX_NAME = 'idx_rag_answer_feedback_review_status'
    ),
    'SELECT 1',
    'ALTER TABLE t_rag_answer_feedback ADD KEY idx_rag_answer_feedback_review_status (review_status)'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;
