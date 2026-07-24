package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagAnswerConfiguration {

    @Bean
    ChatClient ragAnswerChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    RagAnswerService ragAnswerService(MarkdownSimilaritySearchService searchService,
                                      ChatClient ragAnswerChatClient) {
        return new RagAnswerService(searchService, ragAnswerChatClient);
    }
}
