package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Objects;

public record InterviewKnowledgeEvidence(
        String source,
        String documentVersion,
        int chunkOrder,
        List<String> headingPath,
        Double similarity,
        String quote
) {

    public InterviewKnowledgeEvidence {
        source = requireText(source, "评分证据来源不能为空");
        documentVersion = requireText(documentVersion, "评分证据文档版本不能为空");
        if (chunkOrder < 0) {
            throw new IllegalArgumentException("评分证据 Chunk 顺序不能小于 0");
        }
        headingPath = List.copyOf(Objects.requireNonNull(
                headingPath, "评分证据标题路径不能为空"));
        if (headingPath.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("评分证据标题路径不能包含空值");
        }
        quote = requireText(quote, "评分证据引文不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
