ALTER TABLE t_knowledge_document_version
    ADD COLUMN outline_json MEDIUMTEXT NULL COMMENT '无正文章节目录 JSON' AFTER content_length;
