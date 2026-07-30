CREATE TABLE IF NOT EXISTS t_interview_review_attempt (
    id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    review_task_id CHAR(36) NOT NULL,
    question_index TINYINT UNSIGNED NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    answer MEDIUMTEXT NOT NULL,
    evaluation_json MEDIUMTEXT NOT NULL,
    knowledge_grounded TINYINT(1) NOT NULL,
    knowledge_sources_json MEDIUMTEXT NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    rubric_version VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    original_overall_score TINYINT UNSIGNED NOT NULL,
    previous_overall_score TINYINT UNSIGNED NULL,
    score_delta_from_original SMALLINT NOT NULL,
    score_delta_from_previous SMALLINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_review_attempt_task_number (review_task_id, attempt_number),
    KEY idx_interview_review_attempt_session_task_created (
        session_id, review_task_id, created_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
