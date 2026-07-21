CREATE TABLE IF NOT EXISTS t_knowledge_document_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    source VARCHAR(512) NOT NULL COMMENT '文档逻辑来源',
    version CHAR(64) NOT NULL COMMENT '内容 SHA-256 版本',
    content_length INT UNSIGNED NOT NULL COMMENT 'Java 字符长度',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '首次登记时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_version (source, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档版本登记';
