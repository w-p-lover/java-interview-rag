package com.javainterview.rag.knowledge.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class KnowledgeDocumentTaskExecutorConfiguration {

    @Bean(name = "knowledgeDocumentTaskExecutor")
    ThreadPoolTaskExecutor knowledgeDocumentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("knowledge-task-");
        executor.initialize();
        return executor;
    }
}
