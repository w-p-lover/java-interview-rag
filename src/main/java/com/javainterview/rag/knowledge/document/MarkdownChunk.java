package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record MarkdownChunk(String source, int order, int sectionOrder, int headingLevel,
                            List<String> headingPath, String content) {

    public MarkdownChunk {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(headingPath, "标题路径不能为空");
        Objects.requireNonNull(content, "Chunk 正文不能为空");
        if (source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (order < 0) {
            throw new IllegalArgumentException("Chunk 顺序不能小于 0");
        }
        if (sectionOrder < 0) {
            throw new IllegalArgumentException("Section 顺序不能小于 0");
        }
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("标题级别必须在 0 到 6 之间");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Chunk 正文不能为空");
        }
        headingPath = List.copyOf(headingPath);
    }
}
