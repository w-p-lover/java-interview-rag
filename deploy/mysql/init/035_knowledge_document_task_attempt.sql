ALTER TABLE t_knowledge_document_task
    ADD COLUMN attempt_id CHAR(36) NULL COMMENT '当前执行尝试 ID，用于拒绝旧 Worker 迟到写入' AFTER task_id;

UPDATE t_knowledge_document_task
SET attempt_id = UUID()
WHERE attempt_id IS NULL;

ALTER TABLE t_knowledge_document_task
    MODIFY COLUMN attempt_id CHAR(36) NOT NULL COMMENT '当前执行尝试 ID，用于拒绝旧 Worker 迟到写入';
