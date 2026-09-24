package com.javainterview.rag.modelprovider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

public interface ModelProviderClientFactory {
    ChatModel createChatModel(ModelProviderRuntimeConfig provider);
    EmbeddingModel createEmbeddingModel(ModelProviderRuntimeConfig provider);
}
