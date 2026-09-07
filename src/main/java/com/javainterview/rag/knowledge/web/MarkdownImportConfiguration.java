package com.javainterview.rag.knowledge.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.document.MarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.CompositeMarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.InMemoryMarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.InMemoryMarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.TrackingMarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import com.javainterview.rag.knowledge.persistence.KnowledgeDocumentVersionMapper;
import com.javainterview.rag.knowledge.persistence.MySqlMarkdownImportRegistry;
import com.javainterview.rag.knowledge.vector.QdrantMarkdownChunkIndexer;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class MarkdownImportConfiguration {

    private static final int MAX_CHUNK_LENGTH = 800;

    @Bean
    KnowledgeIndexVersionMetadata knowledgeIndexVersionMetadata(
            @Value("${java-interview-rag.index.embedding-model:text-embedding-v3}") String embeddingModel,
            @Value("${java-interview-rag.index.embedding-dimensions:1024}") int embeddingDimensions,
            @Value("${java-interview-rag.index.chunk-policy-version:markdown-section-chunker-v3}") String chunkPolicyVersion,
            @Value("${java-interview-rag.index.parser-version:pdf-knowledge-parser-v3}") String parserVersion,
            @Value("${java-interview-rag.index.schema-version:knowledge-index-v2}") String indexSchemaVersion) {
        return new KnowledgeIndexVersionMetadata(embeddingModel, embeddingDimensions,
                chunkPolicyVersion, parserVersion, indexSchemaVersion);
    }

    @Bean
    MarkdownIndexingStateStore markdownIndexingStateStore() {
        return new InMemoryMarkdownIndexingStateStore();
    }

    @Bean
    @Profile("!storage")
    MarkdownImportRegistry markdownImportRegistry() {
        return new InMemoryMarkdownImportRegistry();
    }

    @Bean
    @Profile("storage")
    MarkdownImportRegistry mysqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper mapper,
                                                        ObjectMapper objectMapper) {
        return new MySqlMarkdownImportRegistry(mapper, objectMapper);
    }

    @Bean
    @Primary
    @Profile("!storage")
    MarkdownChunkIndexer markdownChunkIndexer(LuceneMarkdownChunkIndex sparseIndex,
                                              MarkdownIndexingStateStore stateStore) {
        return new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(java.util.List.of(sparseIndex)), stateStore);
    }

    @Bean
    @Primary
    @Profile("storage")
    MarkdownChunkIndexer qdrantMarkdownChunkIndexer(VectorStore vectorStore,
                                                    LuceneMarkdownChunkIndex sparseIndex,
                                                    MarkdownIndexingStateStore stateStore,
                                                    KnowledgeIndexVersionMetadata indexVersion) {
        return new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(java.util.List.of(
                        new QdrantMarkdownChunkIndexer(vectorStore, indexVersion), sparseIndex)), stateStore);
    }

    @Bean
    MarkdownContentImporter markdownContentImporter(MarkdownImportRegistry registry,
                                                     MarkdownChunkIndexer chunkIndexer,
                                                     MarkdownIndexingStateStore stateStore) {
        return new MarkdownContentImporter(registry, MAX_CHUNK_LENGTH, chunkIndexer, stateStore);
    }

    @Bean
    PdfKnowledgeDocumentExtractor pdfKnowledgeDocumentExtractor() {
        return new PdfKnowledgeDocumentExtractor();
    }
}
