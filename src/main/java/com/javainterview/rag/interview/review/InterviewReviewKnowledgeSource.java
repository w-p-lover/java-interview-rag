package com.javainterview.rag.interview.review;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record InterviewReviewKnowledgeSource(
        String source,
        String documentVersion,
        int chunkOrder,
        List<String> headingPath,
        Double similarity
) {

    public InterviewReviewKnowledgeSource {
        source = requireText(source, "复习知识来源不能为空");
        documentVersion = requireText(documentVersion, "复习知识版本不能为空");
        if (chunkOrder < 0) {
            throw new IllegalArgumentException("复习知识切片顺序不能为负数");
        }
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "复习知识标题路径不能为空"));
    }

    public static InterviewReviewKnowledgeSource from(MarkdownSearchResult source) {
        Objects.requireNonNull(source, "复习知识来源不能为空");
        return new InterviewReviewKnowledgeSource(
                source.source(),
                source.documentVersion(),
                source.chunkOrder(),
                source.headingPath(),
                source.similarity()
        );
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
