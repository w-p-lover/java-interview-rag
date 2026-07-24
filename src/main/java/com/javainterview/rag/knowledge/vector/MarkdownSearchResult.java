package com.javainterview.rag.knowledge.vector;

import java.util.List;
import java.util.Objects;

public record MarkdownSearchResult(String content, String source, String documentVersion,
                                   int chunkOrder, List<String> headingPath, Double similarity) {

    public MarkdownSearchResult {
        Objects.requireNonNull(content, "Chunk 正文不能为空");
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(documentVersion, "文档版本不能为空");
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "标题路径不能为空"));
    }
}
