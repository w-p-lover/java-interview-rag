package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class MarkdownSearchConfiguration {

    @Bean
    MarkdownSimilaritySearchService markdownSimilaritySearchService(VectorStore vectorStore) {
        return new MarkdownSimilaritySearchService(vectorStore);
    }
}
