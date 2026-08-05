CREATE TABLE IF NOT EXISTS t_interview_tool_execution_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    execution_id CHAR(36) NOT NULL COMMENT '单次工具执行 UUID',
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    session_id VARCHAR(36) NOT NULL COMMENT '面试会话 ID',
    status VARCHAR(20) NOT NULL COMMENT '执行状态',
    result_count INT UNSIGNED NULL COMMENT '成功返回的任务数量',
    elapsed_millis BIGINT UNSIGNED NOT NULL COMMENT '执行耗时毫秒',
    error_type VARCHAR(255) NULL COMMENT '失败异常类型',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_tool_execution_id (execution_id),
    KEY idx_interview_tool_session_created (session_id, created_at),
    KEY idx_interview_tool_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='面试工具执行日志';
