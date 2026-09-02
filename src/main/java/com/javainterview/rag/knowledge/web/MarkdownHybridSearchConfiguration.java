package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class MarkdownHybridSearchConfiguration {

    @Bean
    MarkdownHybridSearchService markdownHybridSearchService(
            MarkdownSimilaritySearchService denseSearchService,
            MarkdownSparseSearchService sparseSearchService) {
        return new MarkdownHybridSearchService(denseSearchService, sparseSearchService);
    }
}
