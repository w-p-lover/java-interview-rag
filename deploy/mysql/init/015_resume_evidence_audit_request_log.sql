CREATE TABLE IF NOT EXISTS t_resume_evidence_audit_request_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id CHAR(36) NOT NULL COMMENT '单次简历证据审计请求 UUID',
    role_name VARCHAR(60) NOT NULL COMMENT '审计角色名称',
    prompt_version VARCHAR(100) NOT NULL COMMENT 'Prompt 版本',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    status VARCHAR(20) NOT NULL COMMENT '请求状态',
    prompt_tokens INT UNSIGNED NULL COMMENT '输入 Token',
    completion_tokens INT UNSIGNED NULL COMMENT '输出 Token',
    total_tokens INT UNSIGNED NULL COMMENT '总 Token',
    elapsed_millis BIGINT UNSIGNED NOT NULL COMMENT '请求总耗时毫秒',
    error_type VARCHAR(255) NULL COMMENT '失败异常类型',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_audit_request_id (request_id),
    KEY idx_resume_audit_status_created (status, created_at),
    KEY idx_resume_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='简历证据审计请求日志';
