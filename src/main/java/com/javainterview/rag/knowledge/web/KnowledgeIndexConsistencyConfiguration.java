package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.consistency.KnowledgeIndexConsistencyService;
import com.javainterview.rag.knowledge.consistency.KnowledgeVectorIndexInspector;
import com.javainterview.rag.knowledge.consistency.QdrantKnowledgeVectorIndexInspector;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;
import io.qdrant.client.QdrantClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class KnowledgeIndexConsistencyConfiguration {

    @Bean
    KnowledgeVectorIndexInspector knowledgeVectorIndexInspector(
            QdrantClient client,
            @Value("${spring.ai.vectorstore.qdrant.collection-name}") String collectionName,
            KnowledgeIndexVersionMetadata indexVersion) {
        return new QdrantKnowledgeVectorIndexInspector(client, collectionName, indexVersion);
    }

    @Bean
    KnowledgeIndexConsistencyService knowledgeIndexConsistencyService(
            MarkdownImportRegistry importRegistry,
            MarkdownIndexingStateStore indexingStateStore,
            KnowledgeVectorIndexInspector vectorIndexInspector,
            LuceneMarkdownChunkIndex sparseIndex,
            MarkdownContentImporter contentImporter) {
        return new KnowledgeIndexConsistencyService(
                importRegistry, indexingStateStore, vectorIndexInspector, sparseIndex, contentImporter);
    }
}
