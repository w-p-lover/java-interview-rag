ALTER TABLE t_knowledge_bank_generation_task
    ADD COLUMN attempt_id VARCHAR(36) NULL COMMENT '当前执行 attempt ID' AFTER task_json,
    ADD COLUMN worker_id VARCHAR(100) NULL COMMENT '当前执行 Worker ID' AFTER attempt_id,
    ADD COLUMN started_at DATETIME(6) NULL COMMENT '当前 attempt 开始时间' AFTER worker_id,
    ADD COLUMN last_heartbeat_at DATETIME(6) NULL COMMENT '当前 attempt 最后心跳时间' AFTER started_at,
    ADD COLUMN recovery_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失联自动恢复次数' AFTER last_heartbeat_at,
    ADD KEY idx_bank_generation_lease (status, last_heartbeat_at);

UPDATE t_knowledge_bank_generation_task
SET attempt_id = JSON_UNQUOTE(JSON_EXTRACT(task_json, '$.attemptId'))
WHERE attempt_id IS NULL
  AND JSON_EXTRACT(task_json, '$.attemptId') IS NOT NULL;
