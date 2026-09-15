package com.javainterview.rag.knowledge.document;

import java.util.Objects;

/** 描述一组可互相兼容的知识索引生成契约。 */
public record KnowledgeIndexVersionMetadata(String embeddingModel, int embeddingDimensions,
                                            String chunkPolicyVersion, String parserVersion,
                                            String indexSchemaVersion) {

    public KnowledgeIndexVersionMetadata {
        embeddingModel = requireText(embeddingModel, "Embedding 模型不能为空");
        if (embeddingDimensions <= 0) {
            throw new IllegalArgumentException("Embedding 维度必须大于 0");
        }
        chunkPolicyVersion = requireText(chunkPolicyVersion, "Chunk 策略版本不能为空");
        parserVersion = requireText(parserVersion, "解析器版本不能为空");
        indexSchemaVersion = requireText(indexSchemaVersion, "索引 Schema 版本不能为空");
    }

    public static KnowledgeIndexVersionMetadata current() {
        return new KnowledgeIndexVersionMetadata(
                "text-embedding-v3", 1024, "markdown-section-chunker-v3",
                "pdf-knowledge-parser-v5", "knowledge-index-v2");
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
