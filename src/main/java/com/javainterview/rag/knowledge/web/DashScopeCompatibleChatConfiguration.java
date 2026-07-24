package com.javainterview.rag.knowledge.web;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class DashScopeCompatibleChatConfiguration {

    @Bean
    ChatModel dashScopeCompatibleChatModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.chat.options.temperature}") Double temperature) {
        Assert.hasText(apiKey, "DashScope API Key 不能为空");
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(new SimpleApiKey(apiKey))
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }
}
