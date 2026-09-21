ALTER TABLE t_interview_session
    ADD COLUMN resume_version_id CHAR(36) NULL COMMENT '创建时绑定的简历不可变版本 ID' AFTER target_role,
    ADD KEY idx_interview_session_resume_version (resume_version_id);
