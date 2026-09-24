package com.javainterview.rag.modelprovider;

public record ModelProviderRequest(
        String id,
        String name,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        Integer embeddingDimensions,
        Double temperature,
        boolean enabled,
        Long version) {
}
