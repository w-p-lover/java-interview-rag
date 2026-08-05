CREATE TABLE IF NOT EXISTS t_interview_assistant_request_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id CHAR(36) NOT NULL COMMENT '单次复习助手请求 UUID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    status VARCHAR(20) NOT NULL COMMENT '请求状态',
    tool_execution_count INT UNSIGNED NOT NULL COMMENT '工具方法进入次数',
    prompt_tokens INT UNSIGNED NULL COMMENT '整次请求累计输入 Token',
    completion_tokens INT UNSIGNED NULL COMMENT '整次请求累计输出 Token',
    total_tokens INT UNSIGNED NULL COMMENT '整次请求累计 Token',
    elapsed_millis BIGINT UNSIGNED NOT NULL COMMENT '请求总耗时毫秒',
    final_response_present TINYINT(1) NOT NULL COMMENT '是否存在非空最终响应',
    error_type VARCHAR(255) NULL COMMENT '失败异常类型',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_assistant_request_id (request_id),
    KEY idx_interview_assistant_status_created (status, created_at),
    KEY idx_interview_assistant_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='复习助手请求日志';

ALTER TABLE t_interview_tool_execution_log
    ADD COLUMN assistant_request_id CHAR(36) NULL COMMENT '关联的复习助手请求 UUID' AFTER execution_id,
    ADD KEY idx_interview_tool_assistant_request (assistant_request_id, created_at);
