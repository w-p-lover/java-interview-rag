package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalDeadline;
import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class MarkdownSearchConfiguration {

    @Bean
    MarkdownSimilaritySearchService markdownSimilaritySearchService(
            VectorStore vectorStore,
            @Qualifier("denseRetrievalDeadline") KnowledgeRetrievalDeadline deadline) {
        return new MarkdownSimilaritySearchService(vectorStore, deadline);
    }

    @Bean
    MarkdownAdjacentChunkExpander markdownAdjacentChunkExpander(VectorStore vectorStore) {
        return new MarkdownAdjacentChunkExpander(vectorStore);
    }
}
