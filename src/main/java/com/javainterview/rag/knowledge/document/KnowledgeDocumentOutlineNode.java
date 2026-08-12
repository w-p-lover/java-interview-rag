package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record KnowledgeDocumentOutlineNode(
        String title,
        int level,
        Integer firstChunkOrder,
        int directChunkCount,
        List<KnowledgeDocumentOutlineNode> children
) {

    public KnowledgeDocumentOutlineNode {
        Objects.requireNonNull(title, "章节标题不能为空");
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("章节级别必须在 1 到 6 之间");
        }
        if (firstChunkOrder != null && firstChunkOrder < 0) {
            throw new IllegalArgumentException("首个 Chunk 顺序不能小于 0");
        }
        if (directChunkCount < 0) {
            throw new IllegalArgumentException("直属 Chunk 数量不能小于 0");
        }
        children = List.copyOf(Objects.requireNonNull(children, "子章节不能为空"));
    }
}
