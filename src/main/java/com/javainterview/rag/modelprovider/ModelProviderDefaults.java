package com.javainterview.rag.modelprovider;

import java.time.LocalDateTime;

public record ModelProviderDefaults(
        String chatProviderId,
        String embeddingProviderId,
        long version,
        LocalDateTime updatedAt) {

    public ModelProviderDefaults {
        if (chatProviderId == null || chatProviderId.isBlank()) {
            throw new IllegalArgumentException("默认 Chat Provider 不能为空");
        }
        if (embeddingProviderId == null || embeddingProviderId.isBlank()) {
            throw new IllegalArgumentException("默认 Embedding Provider 不能为空");
        }
        chatProviderId = chatProviderId.strip();
        embeddingProviderId = embeddingProviderId.strip();
        if (version < 0) {
            throw new IllegalArgumentException("Provider 默认值版本不能小于 0");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Provider 默认值更新时间不能为空");
        }
    }
}
