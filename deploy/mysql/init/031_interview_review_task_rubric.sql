ALTER TABLE t_interview_review_task
    ADD COLUMN rubric_version VARCHAR(100) NULL COMMENT '专项评分规则版本' AFTER objective,
    ADD COLUMN weak_dimensions_json TEXT NULL COMMENT '低分专项维度 JSON' AFTER rubric_version;
