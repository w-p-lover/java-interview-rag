package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagRetrievalSource(String source, String documentVersion, int chunkOrder,
                                 List<String> headingPath, Double similarity) {

    public RagRetrievalSource {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(documentVersion, "文档版本不能为空");
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "标题路径不能为空"));
    }

    public static RagRetrievalSource from(MarkdownSearchResult result) {
        return new RagRetrievalSource(result.source(), result.documentVersion(), result.chunkOrder(),
                result.headingPath(), result.similarity());
    }
}
