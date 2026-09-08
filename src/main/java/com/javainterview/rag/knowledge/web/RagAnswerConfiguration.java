package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.persistence.MySqlRagRetrievalLogStore;
import com.javainterview.rag.knowledge.persistence.RagRetrievalLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagAnswerConfiguration {

    @Bean
    ChatClient ragAnswerChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    RagRetrievalLogStore ragRetrievalLogStore(RagRetrievalLogMapper mapper, ObjectMapper objectMapper) {
        return new MySqlRagRetrievalLogStore(mapper, objectMapper);
    }

    @Bean
    RagAnswerService ragAnswerService(MarkdownSimilaritySearchService searchService,
                                      MarkdownHybridSearchService hybridSearchService,
                                      ChatClient ragAnswerChatClient, RagRetrievalLogStore logStore,
                                      @Value("${java-interview-rag.answer.timeout:60s}") Duration answerTimeout) {
        // 固定集对照显示相邻扩展增加噪声且无证据收益，生产回答暂时关闭扩展。
        return new RagAnswerService(searchService, hybridSearchService, null,
                ragAnswerChatClient, logStore, answerTimeout);
    }
}
