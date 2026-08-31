ALTER TABLE t_interview_session
    ADD COLUMN session_mode VARCHAR(32) NOT NULL DEFAULT 'STANDARD' AFTER target_role;
