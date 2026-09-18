ALTER TABLE t_interview_session
    ADD COLUMN request_id VARCHAR(100) NULL COMMENT '正式创建请求幂等 ID' AFTER id,
    ADD UNIQUE KEY uk_interview_session_request_id (request_id);
