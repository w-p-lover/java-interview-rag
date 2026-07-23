ALTER TABLE t_knowledge_document_version
    MODIFY COLUMN source VARCHAR(512)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL COMMENT '文档逻辑来源';
