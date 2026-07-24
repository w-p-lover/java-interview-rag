CREATE TABLE IF NOT EXISTS t_rag_retrieval_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    question VARCHAR(2000) NOT NULL COMMENT '用户问题',
    top_k INT UNSIGNED NOT NULL COMMENT '检索数量',
    similarity_threshold DECIMAL(4,3) NOT NULL COMMENT '相似度阈值',
    prompt_version VARCHAR(64) NOT NULL COMMENT '提示词版本',
    status VARCHAR(32) NOT NULL COMMENT '回答状态',
    grounded TINYINT(1) NOT NULL COMMENT '是否具有可靠依据',
    retrieved_sources_json MEDIUMTEXT NOT NULL COMMENT '命中来源元数据 JSON',
    elapsed_millis BIGINT UNSIGNED NOT NULL COMMENT '回答总耗时毫秒',
    error_type VARCHAR(255) NULL COMMENT '失败类型',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_created_at (created_at),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 召回日志';
