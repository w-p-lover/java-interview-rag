CREATE TABLE IF NOT EXISTS t_resume_audit_evaluation_run (
    run_id CHAR(36) NOT NULL COMMENT '评估运行 ID',
    contract_key VARCHAR(190) NOT NULL COMMENT '数据集、角色、Prompt 和模型组成的防重复契约',
    dataset_version VARCHAR(100) NOT NULL COMMENT '固定评估集版本',
    role_name VARCHAR(100) NOT NULL COMMENT '评估角色',
    prompt_version VARCHAR(100) NOT NULL COMMENT 'Prompt 版本',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    status VARCHAR(32) NOT NULL COMMENT 'QUEUED/RUNNING/SUCCEEDED/PARTIAL_FAILED',
    total_case_count INT UNSIGNED NOT NULL COMMENT '固定样本总数',
    case_results_json MEDIUMTEXT NOT NULL COMMENT '逐样本脱敏结果，不保存简历正文',
    error_type VARCHAR(100) NULL COMMENT '运行级失败类型',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (run_id),
    UNIQUE KEY uk_resume_audit_evaluation_contract (contract_key),
    KEY idx_resume_audit_evaluation_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='可恢复的简历审计固定评估运行';
