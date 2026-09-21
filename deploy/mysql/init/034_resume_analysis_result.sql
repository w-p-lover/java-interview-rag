CREATE TABLE IF NOT EXISTS t_resume_analysis_result (
    id CHAR(36) NOT NULL COMMENT '简历分析结果 ID',
    resume_version_id CHAR(36) NOT NULL COMMENT '关联的不可变简历版本 ID',
    analysis_type VARCHAR(40) NOT NULL COMMENT '分析类型',
    input_fingerprint CHAR(64) NOT NULL COMMENT '输入与运行契约 SHA-256',
    contract_version VARCHAR(100) NOT NULL COMMENT 'Prompt 或策略版本',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称，本地策略为 LOCAL',
    result_json MEDIUMTEXT NOT NULL COMMENT '成功分析结果 JSON',
    created_at DATETIME(6) NOT NULL COMMENT '首次成功时间',
    updated_at DATETIME(6) NOT NULL COMMENT '最近成功时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_analysis_contract (
        resume_version_id, analysis_type, input_fingerprint
    ),
    KEY idx_resume_analysis_latest (resume_version_id, analysis_type, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按不可变简历版本保存的分析结果';
