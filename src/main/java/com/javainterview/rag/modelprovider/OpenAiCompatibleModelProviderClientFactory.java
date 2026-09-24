package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.web.DashScopeCompatibleChatConfiguration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

public class OpenAiCompatibleModelProviderClientFactory implements ModelProviderClientFactory {
    private final RestClient.Builder restClientBuilder;
    private final RetryTemplate embeddingRetryTemplate;

    public OpenAiCompatibleModelProviderClientFactory(
            RestClient.Builder restClientBuilder,
            RetryTemplate embeddingRetryTemplate) {
        this.restClientBuilder = restClientBuilder;
        this.embeddingRetryTemplate = embeddingRetryTemplate;
    }

    @Override
    public ChatModel createChatModel(ModelProviderRuntimeConfig provider) {
        if (provider.chatModel() == null) {
            throw new IllegalArgumentException("Provider 未配置 Chat 模型");
        }
        return OpenAiChatModel.builder()
                .openAiApi(api(provider))
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(provider.chatModel())
                        .temperature(provider.temperature() == null ? 0.2 : provider.temperature())
                        .build())
                .retryTemplate(DashScopeCompatibleChatConfiguration.limitedChatRetryTemplate())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(ModelProviderRuntimeConfig provider) {
        if (provider.embeddingModel() == null || provider.embeddingDimensions() == null) {
            throw new IllegalArgumentException("Provider 未配置 Embedding 模型和维度");
        }
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(provider.embeddingModel())
                .dimensions(provider.embeddingDimensions())
                .build();
        return new OpenAiEmbeddingModel(api(provider), MetadataMode.EMBED,
                options, embeddingRetryTemplate);
    }

    private OpenAiApi api(ModelProviderRuntimeConfig provider) {
        return OpenAiApi.builder()
                .baseUrl(provider.baseUrl())
                .apiKey(new SimpleApiKey(provider.apiKey()))
                .restClientBuilder(restClientBuilder.clone())
                .responseErrorHandler(DashScopeCompatibleChatConfiguration.limitedChatResponseErrorHandler())
                .build();
    }
}
