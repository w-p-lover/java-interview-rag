package com.javainterview.rag.knowledge.document;

import java.util.UUID;

public record MarkdownIndexBuild(String generation, String attemptId) {

    public MarkdownIndexBuild {
        if (generation == null || generation.isBlank()) {
            throw new IllegalArgumentException("索引代际不能为空");
        }
        generation = generation.strip();
        attemptId = attemptId == null || attemptId.isBlank() ? null : attemptId.strip();
    }

    public static MarkdownIndexBuild create(String attemptId) {
        return new MarkdownIndexBuild(UUID.randomUUID().toString(), attemptId);
    }
}
