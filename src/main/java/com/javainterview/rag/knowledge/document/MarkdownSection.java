package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record MarkdownSection(String source, int order, int headingLevel, List<String> headingPath,
                              String content) {

    public MarkdownSection {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(headingPath, "标题路径不能为空");
        Objects.requireNonNull(content, "正文内容不能为空");
        if (order < 0) {
            throw new IllegalArgumentException("段落顺序不能小于 0");
        }
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("标题级别必须在 0 到 6 之间");
        }
        headingPath = List.copyOf(headingPath);
    }
}
