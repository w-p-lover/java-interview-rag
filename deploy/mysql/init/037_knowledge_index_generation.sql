ALTER TABLE t_knowledge_indexing_state
    ADD COLUMN published_generation VARCHAR(36) NULL COMMENT '当前可检索代际' AFTER error_type,
    ADD COLUMN building_generation VARCHAR(36) NULL COMMENT '正在构建的临时代际' AFTER published_generation,
    ADD COLUMN building_chunk_count INT UNSIGNED NULL COMMENT '临时代际预期 Chunk 数量' AFTER building_generation,
    ADD COLUMN attempt_id VARCHAR(36) NULL COMMENT '本次构建任务 attemptId' AFTER building_chunk_count,
    ADD KEY idx_indexing_state_building_generation (building_generation);

UPDATE t_knowledge_indexing_state
SET published_generation = NULL;

INSERT INTO t_knowledge_indexing_state
    (source, document_version, chunk_count, status, error_type, published_generation)
SELECT current_version.source,
       current_version.version,
       COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(
           current_version.outline_json, '$.chunkCount')) AS UNSIGNED), 0),
       'READY',
       NULL,
       'legacy'
FROM t_knowledge_document_version current_version
LEFT JOIN t_knowledge_document_version newer_version
    ON BINARY newer_version.source = BINARY current_version.source
    AND (newer_version.activated_at > current_version.activated_at
        OR (newer_version.activated_at = current_version.activated_at
            AND newer_version.id > current_version.id))
LEFT JOIN t_knowledge_indexing_state state
    ON BINARY state.source = BINARY current_version.source
    AND state.document_version = current_version.version
WHERE newer_version.id IS NULL
  AND state.source IS NULL;

UPDATE t_knowledge_indexing_state state
JOIN (
    SELECT current_version.source, current_version.version
    FROM t_knowledge_document_version current_version
    LEFT JOIN t_knowledge_document_version newer_version
        ON BINARY newer_version.source = BINARY current_version.source
        AND (newer_version.activated_at > current_version.activated_at
            OR (newer_version.activated_at = current_version.activated_at
                AND newer_version.id > current_version.id))
    WHERE newer_version.id IS NULL
) current_document
    ON BINARY current_document.source = BINARY state.source
    AND current_document.version = state.document_version
SET state.published_generation = 'legacy'
WHERE state.status = 'READY';
