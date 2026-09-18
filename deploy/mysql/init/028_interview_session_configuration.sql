ALTER TABLE t_interview_session
    ADD COLUMN configuration_json MEDIUMTEXT NULL COMMENT '确认后的面试配置与题目来源快照 JSON' AFTER session_mode;
