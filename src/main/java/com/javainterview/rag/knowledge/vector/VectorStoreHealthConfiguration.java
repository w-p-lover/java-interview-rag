package com.javainterview.rag.knowledge.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
@Slf4j
public class VectorStoreHealthConfiguration {

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
