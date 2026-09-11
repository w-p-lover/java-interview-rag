CREATE TABLE IF NOT EXISTS t_knowledge_indexing_state (
    source VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文档逻辑来源',
    document_version CHAR(64) NOT NULL COMMENT '内容 SHA-256 版本',
    chunk_count INT UNSIGNED NOT NULL COMMENT '本次索引 Chunk 数量',
    status VARCHAR(16) NOT NULL COMMENT 'INDEXING、READY 或 FAILED',
    error_type VARCHAR(128) NULL COMMENT '失败异常类型',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '最后状态更新时间',
    PRIMARY KEY (source, document_version),
    KEY idx_indexing_state_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识索引编排状态';
