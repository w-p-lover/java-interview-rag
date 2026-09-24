package com.javainterview.rag.modelprovider;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

public class RoutingEmbeddingModel implements EmbeddingModel {
    private final ModelProviderRegistry registry;

    public RoutingEmbeddingModel(ModelProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return registry.callEmbedding(model -> model.call(request));
    }

    @Override
    public float[] embed(Document document) {
        return registry.callEmbedding(model -> model.embed(document));
    }

    @Override
    public int dimensions() {
        return registry.callEmbedding(EmbeddingModel::dimensions);
    }
}
