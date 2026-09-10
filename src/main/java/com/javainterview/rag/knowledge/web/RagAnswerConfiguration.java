package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.rag.RagAnswerCache;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.persistence.MySqlRagRetrievalLogStore;
import com.javainterview.rag.knowledge.persistence.RagRetrievalLogMapper;
import com.javainterview.rag.knowledge.persistence.RedisRagAnswerCache;
import com.javainterview.rag.knowledge.persistence.MicrometerRagAnswerCacheObserver;
import com.javainterview.rag.knowledge.rag.RagAnswerCacheObserver;
import com.javainterview.rag.knowledge.persistence.KnowledgeReviewTaskMapper;
import com.javainterview.rag.knowledge.persistence.MySqlKnowledgeReviewTaskStore;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskService;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskStore;
import com.javainterview.rag.knowledge.review.KnowledgeReviewPathService;
import com.javainterview.rag.knowledge.persistence.KnowledgeReviewAttemptMapper;
import com.javainterview.rag.knowledge.persistence.MySqlKnowledgeReviewAttemptStore;
import com.javainterview.rag.knowledge.review.KnowledgeReviewAttemptStore;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagAnswerConfiguration {

    @Bean
    KnowledgeReviewTaskStore knowledgeReviewTaskStore(KnowledgeReviewTaskMapper mapper, ObjectMapper objectMapper) {
        return new MySqlKnowledgeReviewTaskStore(mapper, objectMapper);
    }

    @Bean
    KnowledgeReviewTaskService knowledgeReviewTaskService(KnowledgeReviewTaskStore store,
                                                          KnowledgeReviewAttemptStore attemptStore,
                                                          InterviewAnswerEvaluator evaluator) {
        return new KnowledgeReviewTaskService(store, attemptStore, evaluator);
    }

    @Bean
    KnowledgeReviewPathService knowledgeReviewPathService(KnowledgeReviewTaskStore store,
                                                          KnowledgeReviewAttemptStore attemptStore) {
        return new KnowledgeReviewPathService(store, attemptStore);
    }

    @Bean
    KnowledgeReviewAttemptStore knowledgeReviewAttemptStore(KnowledgeReviewAttemptMapper mapper, ObjectMapper objectMapper) {
        return new MySqlKnowledgeReviewAttemptStore(mapper, objectMapper);
    }

    @Bean
    ChatClient ragAnswerChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    RagRetrievalLogStore ragRetrievalLogStore(RagRetrievalLogMapper mapper, ObjectMapper objectMapper) {
        return new MySqlRagRetrievalLogStore(mapper, objectMapper);
    }

    @Bean
    RagAnswerCache ragAnswerCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                  @Value("${java-interview-rag.answer.cache.ttl:10m}") Duration ttl,
                                  KnowledgeIndexVersionMetadata indexVersion) {
        return new RedisRagAnswerCache(redisTemplate, objectMapper, ttl);
    }

    @Bean
    RagAnswerCacheObserver ragAnswerCacheObserver(MeterRegistry meterRegistry) {
        return new MicrometerRagAnswerCacheObserver(meterRegistry);
    }

    @Bean(destroyMethod = "shutdown")
    ThreadPoolTaskExecutor ragAnswerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("rag-answer-");
        executor.initialize();
        return executor;
    }

    @Bean
    RagAnswerService ragAnswerService(MarkdownSimilaritySearchService searchService,
                                      MarkdownHybridSearchService hybridSearchService,
                                      ChatClient ragAnswerChatClient, RagRetrievalLogStore logStore,
                                      @Value("${java-interview-rag.answer.timeout:60s}") Duration answerTimeout,
                                      ThreadPoolTaskExecutor ragAnswerExecutor,
                                      RagAnswerCache answerCache,
                                      KnowledgeIndexVersionMetadata indexVersion,
                                      MarkdownImportRegistry importRegistry,
                                      RagAnswerCacheObserver cacheObserver) {
        // 固定集对照显示相邻扩展增加噪声且无证据收益，生产回答暂时关闭扩展。
        return new RagAnswerService(searchService, hybridSearchService, null,
                ragAnswerChatClient, logStore, answerTimeout, ragAnswerExecutor, 4,
                answerCache, indexVersion.toString(), () -> knowledgeVersionKey(importRegistry), cacheObserver);
    }

    private String knowledgeVersionKey(MarkdownImportRegistry importRegistry) {
        return importRegistry.findCurrentDocuments().stream()
                .map(document -> document.source() + "\u0000" + document.version())
                .sorted()
                .collect(java.util.stream.Collectors.joining("\u0001"));
    }
}
