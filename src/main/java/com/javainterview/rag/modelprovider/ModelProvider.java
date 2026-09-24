package com.javainterview.rag.modelprovider;

import java.time.LocalDateTime;
import java.util.Objects;

public record ModelProvider(
        String id,
        String name,
        String baseUrl,
        String apiKeyNonce,
        String apiKeyCiphertext,
        String chatModel,
        String embeddingModel,
        Integer embeddingDimensions,
        Double temperature,
        boolean enabled,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ModelProvider {
        id = requireText(id, "Provider ID 不能为空");
        name = requireText(name, "Provider 名称不能为空");
        baseUrl = requireText(baseUrl, "Provider 地址不能为空");
        apiKeyNonce = requireText(apiKeyNonce, "Provider 密钥 nonce 不能为空");
        apiKeyCiphertext = requireText(apiKeyCiphertext, "Provider 密钥密文不能为空");
        chatModel = optionalText(chatModel);
        embeddingModel = optionalText(embeddingModel);
        if (chatModel == null && embeddingModel == null) {
            throw new IllegalArgumentException("Provider 至少需要配置一种模型能力");
        }
        if (embeddingModel != null && (embeddingDimensions == null || embeddingDimensions <= 0)) {
            throw new IllegalArgumentException("Embedding Provider 必须配置有效维度");
        }
        if (embeddingModel == null) {
            embeddingDimensions = null;
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("模型温度必须在 0 到 2 之间");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Provider 版本不能小于 0");
        }
        createdAt = Objects.requireNonNull(createdAt, "Provider 创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "Provider 更新时间不能为空");
    }

    public boolean supportsChat() {
        return chatModel != null;
    }

    public boolean supportsEmbedding() {
        return embeddingModel != null;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
