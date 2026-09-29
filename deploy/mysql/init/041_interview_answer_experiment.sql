CREATE TABLE IF NOT EXISTS t_interview_answer_experiment_run (
 run_id CHAR(36) NOT NULL, name VARCHAR(100) NOT NULL, dataset_version VARCHAR(100) NOT NULL,
 prompt_version VARCHAR(100) NOT NULL, rubric_version VARCHAR(100) NOT NULL, model_name VARCHAR(200) NOT NULL,
 contract_hash CHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, total_case_count INT UNSIGNED NOT NULL,
 case_results_json MEDIUMTEXT NOT NULL, report_json MEDIUMTEXT NULL, error_type VARCHAR(100) NULL,
 baseline_run_id CHAR(36) NULL, decision VARCHAR(16) NULL, decision_note VARCHAR(500) NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, completed_at DATETIME(6) NULL, decided_at DATETIME(6) NULL,
 PRIMARY KEY(run_id), UNIQUE KEY uk_interview_answer_experiment_contract(contract_hash),
 KEY idx_interview_answer_experiment_status_created(status,created_at),
 CONSTRAINT fk_interview_answer_experiment_baseline FOREIGN KEY(baseline_run_id) REFERENCES t_interview_answer_experiment_run(run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可恢复、可比较的回答与引用实验';
