ALTER TABLE t_knowledge_document_version
    ADD COLUMN content LONGTEXT NULL COMMENT '用于人工回退的规范化正文快照' AFTER outline_json,
    ADD COLUMN sections_json JSON NULL COMMENT '用于保留 PDF 页码和块类型的章节快照' AFTER content,
    ADD COLUMN activated_at DATETIME(6) NULL COMMENT '最近激活为当前版本的时间' AFTER sections_json;

UPDATE t_knowledge_document_version
SET activated_at = created_at
WHERE activated_at IS NULL;

ALTER TABLE t_knowledge_document_version
    MODIFY COLUMN activated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD KEY idx_knowledge_document_current (source, activated_at, id);
