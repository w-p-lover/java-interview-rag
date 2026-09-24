package com.javainterview.rag.modelprovider;

public record ModelProviderRuntimeConfig(
        String id,
        long version,
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        Integer embeddingDimensions,
        Double temperature) {

    public String chatContract() {
        return id + "/" + chatModel + "#" + version;
    }
}
