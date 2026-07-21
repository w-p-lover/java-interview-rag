package com.javainterview.rag.knowledge.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class VectorStoreHealthConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreHealthConfiguration.class);

    @Bean
    ApplicationRunner vectorStoreHealthRunner(VectorStore vectorStore) {
        return args -> {
            try {
                vectorStore.similaritySearch(
                        SearchRequest.builder().query("health-check").topK(1).build());
            } catch (Exception exception) {
                log.warn("Qdrant 启动探测失败，跳过阻塞启动", exception);
            }
        };
    }
}
