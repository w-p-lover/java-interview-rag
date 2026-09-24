CREATE TABLE t_model_provider (
    id VARCHAR(32) NOT NULL COMMENT 'Provider 稳定标识',
    name VARCHAR(100) NOT NULL COMMENT '显示名称',
    base_url VARCHAR(500) NOT NULL COMMENT 'OpenAI-compatible 基础地址',
    api_key_nonce VARCHAR(64) NOT NULL COMMENT 'AES-GCM nonce',
    api_key_ciphertext VARCHAR(1024) NOT NULL COMMENT 'AES-GCM 密文',
    chat_model VARCHAR(100) NULL COMMENT 'Chat 模型名',
    embedding_model VARCHAR(100) NULL COMMENT 'Embedding 模型名',
    embedding_dimensions INT UNSIGNED NULL COMMENT 'Embedding 维度',
    temperature DECIMAL(4,3) NULL COMMENT 'Chat 温度',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_model_provider_enabled (enabled, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='模型 Provider 配置';

CREATE TABLE t_model_provider_defaults (
    id TINYINT UNSIGNED NOT NULL COMMENT '固定为 1',
    chat_provider_id VARCHAR(32) NOT NULL COMMENT '默认 Chat Provider',
    embedding_provider_id VARCHAR(32) NOT NULL COMMENT '默认 Embedding Provider',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_model_default_chat FOREIGN KEY (chat_provider_id) REFERENCES t_model_provider (id),
    CONSTRAINT fk_model_default_embedding FOREIGN KEY (embedding_provider_id) REFERENCES t_model_provider (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='模型 Provider 默认值';

ALTER TABLE t_interview_assistant_request_log
    MODIFY COLUMN model_name VARCHAR(200) NOT NULL COMMENT '模型运行契约';

ALTER TABLE t_resume_evidence_audit_request_log
    MODIFY COLUMN model_name VARCHAR(200) NOT NULL COMMENT '模型运行契约';

ALTER TABLE t_resume_job_match_request_log
    MODIFY COLUMN model_name VARCHAR(200) NOT NULL COMMENT '模型运行契约';

ALTER TABLE t_resume_audit_evaluation_run
    MODIFY COLUMN model_name VARCHAR(200) NOT NULL COMMENT '模型运行契约';

ALTER TABLE t_resume_analysis_result
    MODIFY COLUMN model_name VARCHAR(200) NOT NULL COMMENT '模型运行契约，本地策略为 LOCAL';
