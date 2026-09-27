CREATE TABLE IF NOT EXISTS t_file_asset (
    id CHAR(36) NOT NULL COMMENT '文件 ID',
    purpose VARCHAR(20) NOT NULL COMMENT 'KNOWLEDGE 或 RESUME',
    object_key VARCHAR(255) NOT NULL COMMENT 'local/S3 对象键',
    content_hash CHAR(64) NOT NULL COMMENT '原始字节 SHA-256',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(100) NOT NULL COMMENT '内容类型',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '原始字节数',
    status VARCHAR(20) NOT NULL COMMENT 'UPLOADING、READY、FAILED、DELETE_PENDING 或 DELETED',
    error_type VARCHAR(128) NULL COMMENT '最近一次存储错误类型',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
    deleted_at DATETIME(6) NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_asset_purpose_hash (purpose, content_hash),
    UNIQUE KEY uk_file_asset_object_key (object_key),
    KEY idx_file_asset_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件对象元数据';

ALTER TABLE t_knowledge_document_version
    ADD COLUMN file_id CHAR(36) NULL COMMENT '原始文件 ID' AFTER sections_json,
    ADD KEY idx_knowledge_document_file (file_id),
    ADD CONSTRAINT fk_knowledge_document_file FOREIGN KEY (file_id) REFERENCES t_file_asset (id);

ALTER TABLE t_knowledge_document_task
    ADD COLUMN file_id CHAR(36) NULL COMMENT '任务输入文件 ID' AFTER source,
    ADD COLUMN recovery_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '应用重启恢复次数' AFTER file_id,
    ADD KEY idx_knowledge_task_file (file_id),
    ADD CONSTRAINT fk_knowledge_task_file FOREIGN KEY (file_id) REFERENCES t_file_asset (id);

ALTER TABLE t_resume_version
    ADD COLUMN file_id CHAR(36) NULL COMMENT '用户明确保存的原文件 ID' AFTER file_size,
    ADD KEY idx_resume_version_file (file_id),
    ADD CONSTRAINT fk_resume_version_file FOREIGN KEY (file_id) REFERENCES t_file_asset (id);
