CREATE TABLE IF NOT EXISTS t_interview_session (
    id CHAR(36) NOT NULL COMMENT '面试会话 UUID',
    target_role VARCHAR(100) NOT NULL COMMENT '目标岗位',
    profile_json MEDIUMTEXT NOT NULL COMMENT '确认后的面试准备画像 JSON',
    questions_json MEDIUMTEXT NOT NULL COMMENT '首轮面试题与知识来源 JSON',
    answers_json MEDIUMTEXT NOT NULL COMMENT '按顺序提交的回答 JSON',
    current_question_index TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前待回答题号',
    status VARCHAR(32) NOT NULL COMMENT '会话状态',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_updated_at (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文本面试会话';
