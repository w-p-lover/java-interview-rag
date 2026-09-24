package com.javainterview.rag.modelprovider;

public record ModelProviderDefaultsRequest(
        String chatProviderId,
        String embeddingProviderId,
        Long version) {
}
