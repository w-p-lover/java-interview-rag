ALTER TABLE t_interview_review_task
    ADD COLUMN completed_at DATETIME(6) NULL COMMENT '首次完成复习时间' AFTER created_at;
