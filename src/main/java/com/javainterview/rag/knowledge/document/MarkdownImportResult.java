package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record MarkdownImportResult(MarkdownDocument document, boolean imported,
                                   List<MarkdownChunk> chunks) {

    public MarkdownImportResult {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        chunks = List.copyOf(chunks);
    }
}
