CREATE TABLE IF NOT EXISTS t_knowledge_review_attempt (
    id CHAR(36) NOT NULL,
    task_id CHAR(64) NOT NULL,
    answer TEXT NOT NULL,
    evaluation_json MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_knowledge_review_attempt_task (task_id, created_at),
    CONSTRAINT fk_knowledge_review_attempt_task FOREIGN KEY (task_id)
        REFERENCES t_knowledge_review_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识复习回答评价';
