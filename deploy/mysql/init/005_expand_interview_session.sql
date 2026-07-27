ALTER TABLE t_interview_session
    ADD COLUMN max_question_count TINYINT UNSIGNED NOT NULL DEFAULT 8 COMMENT '整场题数上限' AFTER questions_json,
    ADD COLUMN report_json MEDIUMTEXT NULL COMMENT '完成后的汇总报告 JSON' AFTER answers_json;
