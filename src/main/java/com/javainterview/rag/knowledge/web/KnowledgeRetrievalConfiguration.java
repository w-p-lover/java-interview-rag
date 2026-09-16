package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalDeadline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class KnowledgeRetrievalConfiguration {

    @Bean(name = "denseRetrievalExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskExecutor denseRetrievalExecutor() {
        return retrievalExecutor(4, 16, "dense-retrieval-");
    }

    @Bean(name = "sparseRetrievalExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskExecutor sparseRetrievalExecutor() {
        return retrievalExecutor(2, 8, "sparse-retrieval-");
    }

    @Bean(name = "denseRetrievalDeadline")
    KnowledgeRetrievalDeadline denseRetrievalDeadline(
            @Qualifier("denseRetrievalExecutor") ThreadPoolTaskExecutor executor,
            @Value("${java-interview-rag.retrieval.timeout:5s}") Duration timeout) {
        return new KnowledgeRetrievalDeadline(executor.getThreadPoolExecutor(), timeout);
    }

    @Bean(name = "sparseRetrievalDeadline")
    KnowledgeRetrievalDeadline sparseRetrievalDeadline(
            @Qualifier("sparseRetrievalExecutor") ThreadPoolTaskExecutor executor,
            @Value("${java-interview-rag.retrieval.timeout:5s}") Duration timeout) {
        return new KnowledgeRetrievalDeadline(executor.getThreadPoolExecutor(), timeout);
    }

    private ThreadPoolTaskExecutor retrievalExecutor(int threads, int queueCapacity,
                                                     String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
