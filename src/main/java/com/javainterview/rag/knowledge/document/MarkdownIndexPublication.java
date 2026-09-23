package com.javainterview.rag.knowledge.document;

import java.util.Objects;

public record MarkdownIndexPublication(String source, String documentVersion, String generation,
                                       String buildingGeneration) {

    public static final String LEGACY_GENERATION = "legacy";

    public MarkdownIndexPublication {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(documentVersion, "文档版本不能为空");
        Objects.requireNonNull(generation, "已发布索引代际不能为空");
    }

    public MarkdownIndexPublication(String source, String documentVersion, String generation) {
        this(source, documentVersion, generation, null);
    }

    public boolean legacy() {
        return LEGACY_GENERATION.equals(generation);
    }
}
