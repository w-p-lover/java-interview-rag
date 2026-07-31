CREATE TABLE IF NOT EXISTS t_interview_answer_evaluation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    evaluation_mode VARCHAR(20) NOT NULL COMMENT '评价模式',
    prompt_version VARCHAR(100) NOT NULL COMMENT '提示词版本',
    rubric_version VARCHAR(100) NOT NULL COMMENT '评分规则版本',
    model_name VARCHAR(200) NOT NULL COMMENT '模型名称',
    status VARCHAR(20) NOT NULL COMMENT '评价状态',
    knowledge_grounded TINYINT(1) NULL COMMENT '是否具有可靠知识依据',
    technical_score TINYINT UNSIGNED NULL COMMENT '技术分',
    completeness_score TINYINT UNSIGNED NULL COMMENT '完整性分',
    communication_score TINYINT UNSIGNED NULL COMMENT '表达分',
    overall_score TINYINT UNSIGNED NULL COMMENT '总分',
    elapsed_millis BIGINT UNSIGNED NOT NULL COMMENT '评价耗时毫秒',
    error_type VARCHAR(255) NULL COMMENT '失败类型',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_interview_evaluation_mode_created (evaluation_mode, created_at),
    KEY idx_interview_evaluation_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='面试回答评价日志';
