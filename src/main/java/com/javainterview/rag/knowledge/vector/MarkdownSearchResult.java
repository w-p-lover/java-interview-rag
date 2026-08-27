package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;

import java.util.List;
import java.util.Objects;

public record MarkdownSearchResult(String content, String source, String documentVersion,
                                   int chunkOrder, List<String> headingPath, Double similarity,
                                   Integer pageNumber, KnowledgeBlockType blockType) {

    public MarkdownSearchResult(String content, String source, String documentVersion,
                                int chunkOrder, List<String> headingPath, Double similarity) {
        this(content, source, documentVersion, chunkOrder, headingPath, similarity,
                null, KnowledgeBlockType.TEXT);
    }

    public MarkdownSearchResult {
        Objects.requireNonNull(content, "Chunk 正文不能为空");
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(documentVersion, "文档版本不能为空");
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "标题路径不能为空"));
        Objects.requireNonNull(blockType, "内容块类型不能为空");
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("PDF 页码必须从 1 开始");
        }
    }
}
