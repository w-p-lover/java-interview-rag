CREATE TABLE IF NOT EXISTS t_interview_review_task (
    id CHAR(36) NOT NULL COMMENT '确定性复习任务 UUID',
    session_id CHAR(36) NOT NULL COMMENT '来源面试会话 UUID',
    question_index TINYINT UNSIGNED NOT NULL COMMENT '原面试题号',
    question TEXT NOT NULL COMMENT '原面试题目',
    objective TEXT NOT NULL COMMENT '原考察目标',
    gaps_json MEDIUMTEXT NOT NULL COMMENT '回答遗漏项 JSON',
    improvement_suggestion TEXT NOT NULL COMMENT '改进建议',
    knowledge_grounded TINYINT(1) NOT NULL COMMENT '是否有可靠知识来源',
    knowledge_sources_json MEDIUMTEXT NOT NULL COMMENT '待复习知识来源元数据 JSON',
    status VARCHAR(32) NOT NULL COMMENT '复习任务状态',
    created_at DATETIME(6) NOT NULL COMMENT '任务创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_question (session_id, question_index),
    KEY idx_session_status (session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试复习任务';
