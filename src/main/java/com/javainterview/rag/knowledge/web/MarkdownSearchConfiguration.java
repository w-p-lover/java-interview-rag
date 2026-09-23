package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalDeadline;
import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.PublishedMarkdownIndexFilter;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class MarkdownSearchConfiguration {

    @Bean
    PublishedMarkdownIndexFilter publishedMarkdownIndexFilter(MarkdownIndexingStateStore stateStore) {
        return new PublishedMarkdownIndexFilter(stateStore);
    }

    @Bean
    MarkdownSimilaritySearchService markdownSimilaritySearchService(
            VectorStore vectorStore,
            @Qualifier("denseRetrievalDeadline") KnowledgeRetrievalDeadline deadline,
            PublishedMarkdownIndexFilter publishedFilter) {
        return new MarkdownSimilaritySearchService(vectorStore, deadline, publishedFilter);
    }

    @Bean
    MarkdownAdjacentChunkExpander markdownAdjacentChunkExpander(
            VectorStore vectorStore, PublishedMarkdownIndexFilter publishedFilter) {
        return new MarkdownAdjacentChunkExpander(vectorStore, publishedFilter);
    }
}
