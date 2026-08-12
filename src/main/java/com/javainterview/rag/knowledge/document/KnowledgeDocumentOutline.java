package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record KnowledgeDocumentOutline(
        int headingCount,
        int chunkCount,
        List<KnowledgeDocumentOutlineNode> roots
) {

    public KnowledgeDocumentOutline {
        if (headingCount < 0 || chunkCount < 0) {
            throw new IllegalArgumentException("章节和 Chunk 数量不能小于 0");
        }
        roots = List.copyOf(Objects.requireNonNull(roots, "章节根节点不能为空"));
    }
}
