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
import com.javainterview.rag.knowledge.document.KnowledgeStoredFileParser;
import com.javainterview.rag.file.FileAssetService;
import com.javainterview.rag.knowledge.task.InMemoryKnowledgeDocumentTaskStore;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskService;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskMetrics;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStore;
import com.javainterview.rag.knowledge.persistence.KnowledgeDocumentVersionMapper;
import com.javainterview.rag.knowledge.persistence.KnowledgeIndexingStateMapper;
import com.javainterview.rag.knowledge.persistence.MySqlMarkdownImportRegistry;
import com.javainterview.rag.knowledge.persistence.MySqlMarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.persistence.KnowledgeDocumentTaskMapper;
import com.javainterview.rag.knowledge.persistence.MySqlKnowledgeDocumentTaskStore;
import com.javainterview.rag.knowledge.vector.QdrantMarkdownChunkIndexer;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;
import com.javainterview.rag.knowledge.consistency.KnowledgeVectorIndexInspector;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class MarkdownImportConfiguration {

    @Bean
    KnowledgeIndexVersionMetadata knowledgeIndexVersionMetadata(
            @Value("${java-interview-rag.index.embedding-model:text-embedding-v3}") String embeddingModel,
            @Value("${java-interview-rag.index.embedding-dimensions:1024}") int embeddingDimensions,
            @Value("${java-interview-rag.index.chunk-policy-version:markdown-section-chunker-v5}") String chunkPolicyVersion,
            @Value("${java-interview-rag.index.parser-version:pdf-knowledge-parser-v6}") String parserVersion,
            @Value("${java-interview-rag.index.schema-version:knowledge-index-v2}") String indexSchemaVersion) {
        return new KnowledgeIndexVersionMetadata(embeddingModel, embeddingDimensions,
                chunkPolicyVersion, parserVersion, indexSchemaVersion);
    }

    @Bean
    @Profile("!storage")
    MarkdownIndexingStateStore markdownIndexingStateStore() {
        return new InMemoryMarkdownIndexingStateStore();
    }

    @Bean
    @Profile("storage")
    MarkdownIndexingStateStore mysqlMarkdownIndexingStateStore(KnowledgeIndexingStateMapper mapper) {
        return new MySqlMarkdownIndexingStateStore(mapper);
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
                                                    KnowledgeIndexVersionMetadata indexVersion,
                                                    KnowledgeVectorIndexInspector vectorIndexInspector) {
        return new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(java.util.List.of(
                        new QdrantMarkdownChunkIndexer(vectorStore, indexVersion, vectorIndexInspector),
                        sparseIndex)), stateStore);
    }

    @Bean
    MarkdownContentImporter markdownContentImporter(MarkdownImportRegistry registry,
                                                     MarkdownChunkIndexer chunkIndexer,
                                                     MarkdownIndexingStateStore stateStore,
                                                     @Value("${java-interview-rag.knowledge.chunk.max-tokens:600}")
                                                     int maxChunkTokens) {
        return new MarkdownContentImporter(registry, maxChunkTokens, chunkIndexer, stateStore);
    }

    @Bean
    PdfKnowledgeDocumentExtractor pdfKnowledgeDocumentExtractor() {
        return new PdfKnowledgeDocumentExtractor();
    }

    @Bean
    KnowledgeStoredFileParser knowledgeStoredFileParser(
            FileAssetService files,
            PdfKnowledgeDocumentExtractor pdfExtractor) {
        return new KnowledgeStoredFileParser(files, pdfExtractor);
    }

    @Bean
    @Profile("!storage")
    KnowledgeDocumentTaskStore knowledgeDocumentTaskStore() {
        return new InMemoryKnowledgeDocumentTaskStore();
    }

    @Bean
    @Profile("storage")
    KnowledgeDocumentTaskStore mysqlKnowledgeDocumentTaskStore(KnowledgeDocumentTaskMapper mapper) {
        return new MySqlKnowledgeDocumentTaskStore(mapper);
    }

    @Bean
    KnowledgeDocumentTaskService knowledgeDocumentTaskService(
            MarkdownContentImporter importer,
            KnowledgeDocumentTaskStore taskStore,
            KnowledgeStoredFileParser inputLoader,
            @Qualifier("knowledgeDocumentTaskExecutor") ThreadPoolTaskExecutor executor,
            KnowledgeDocumentTaskMetrics metrics,
            @Qualifier("knowledgeDocumentTaskTimeoutScheduler") ScheduledExecutorService timeoutScheduler,
            @Value("${java-interview-rag.knowledge.task.timeout-ms:600000}") long timeoutMillis) {
        return new KnowledgeDocumentTaskService(
                importer, taskStore, inputLoader, executor, metrics, timeoutScheduler, timeoutMillis);
    }

    @Bean
    @Profile("storage")
    ApplicationRunner recoverKnowledgeDocumentTasksOnRestart(
            KnowledgeDocumentTaskService taskService) {
        return args -> taskService.resumePendingTasks();
    }

    @Bean(name = "knowledgeDocumentTaskTimeoutScheduler", destroyMethod = "shutdownNow")
    ScheduledExecutorService knowledgeDocumentTaskTimeoutScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "knowledge-task-timeout");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
