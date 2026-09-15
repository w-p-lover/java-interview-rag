package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record KnowledgeDocumentSnapshot(MarkdownDocument document, List<MarkdownSection> sections) {
    public KnowledgeDocumentSnapshot {
        document = Objects.requireNonNull(document, "文档快照不能为空");
        sections = List.copyOf(Objects.requireNonNull(sections, "文档章节快照不能为空"));
    }
}
