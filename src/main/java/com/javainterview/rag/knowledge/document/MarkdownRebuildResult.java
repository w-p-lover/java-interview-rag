package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record MarkdownRebuildResult(MarkdownDocument document, List<MarkdownChunk> chunks) {

    public MarkdownRebuildResult {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        chunks = List.copyOf(chunks);
    }
}
