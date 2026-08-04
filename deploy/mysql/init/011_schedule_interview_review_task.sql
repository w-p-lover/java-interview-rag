ALTER TABLE t_interview_review_task
    ADD COLUMN planned_review_date DATE NULL COMMENT '计划复习日期' AFTER status,
    ADD KEY idx_session_status_planned_date (session_id, status, planned_review_date);
