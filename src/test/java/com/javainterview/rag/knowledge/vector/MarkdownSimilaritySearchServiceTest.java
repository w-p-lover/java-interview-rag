package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.document.InMemoryMarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownIndexBuild;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkdownSimilaritySearchServiceTest {

    @Test
    void searchesWithRequestedParametersAndMapsTraceableChunkMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        Document document = Document.builder()
                .id("chunk-id")
                .text("HashMap 在 Java 8 中使用数组、链表和红黑树。")
                .metadata(Map.of(
                        "source", "java/collections.md",
                        "document_version", "version-1",
                        "chunk_order", 2,
                        "heading_path", List.of("Java", "集合", "HashMap"),
                        "page_number", 7,
                        "block_type", "TABLE"
                ))
                .score(0.91)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
        MarkdownSimilaritySearchService service = new MarkdownSimilaritySearchService(vectorStore);

        List<MarkdownSearchResult> results = service.search("HashMap 底层结构", 3, 0.65);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertEquals("HashMap 底层结构", request.getQuery());
        assertEquals(3, request.getTopK());
        assertEquals(0.65, request.getSimilarityThreshold());
        assertEquals(List.of(new MarkdownSearchResult(
                "HashMap 在 Java 8 中使用数组、链表和红黑树。",
                "java/collections.md",
                "version-1",
                2,
                List.of("Java", "集合", "HashMap"),
                0.91,
                7,
                KnowledgeBlockType.TABLE
        )), results);
    }

    @Test
    void rejectsInvalidSearchParametersBeforeCallingVectorStore() {
        MarkdownSimilaritySearchService service = new MarkdownSimilaritySearchService(mock(VectorStore.class));

        assertEquals("检索问题不能为空",
                assertThrows(IllegalArgumentException.class,
                        () -> service.search(" ", 5, 0.5)).getMessage());
        assertEquals("topK 必须在 1 到 20 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.search("HashMap", 21, 0.5)).getMessage());
        assertEquals("相似度阈值必须在 0 到 1 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.search("HashMap", 5, 1.1)).getMessage());
    }

    @Test
    void stopsWaitingWhenDenseRetrievalExceedsItsDeadline() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            Thread.sleep(200);
            return List.of();
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            MarkdownSimilaritySearchService service = new MarkdownSimilaritySearchService(
                    vectorStore, new KnowledgeRetrievalDeadline(executor, Duration.ofMillis(20)));

            KnowledgeRetrievalTimeoutException exception = assertThrows(
                    KnowledgeRetrievalTimeoutException.class,
                    () -> service.search("HashMap", 5, 0.6));

            org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Dense 向量检索"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void excludesTemporaryGenerationUntilThePublicationPointerSwitches() {
        VectorStore vectorStore = mock(VectorStore.class);
        MarkdownDocument source = MarkdownDocument.create("java/atomic.md", "同一快照");
        Document legacy = document(source, "oldPublishedMarker", null);
        Document temporary = document(source, "newTemporaryMarker", "generation-2");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(temporary, legacy), List.of(temporary, legacy));
        InMemoryMarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        stateStore.markReady(source, 1);
        MarkdownIndexBuild build = new MarkdownIndexBuild("generation-2", "attempt-2");
        stateStore.beginBuild(source, 1, build);
        MarkdownSimilaritySearchService service = new MarkdownSimilaritySearchService(
                vectorStore, null, new PublishedMarkdownIndexFilter(stateStore));

        assertEquals(List.of("oldPublishedMarker"), service.search("marker", 5, 0.0).stream()
                .map(MarkdownSearchResult::content).toList());

        assertTrue(stateStore.publishBuild(source, 1, build));
        assertEquals(List.of("newTemporaryMarker"), service.search("marker", 5, 0.0).stream()
                .map(MarkdownSearchResult::content).toList());
        ArgumentCaptor<SearchRequest> requests = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(2)).similaritySearch(requests.capture());
        assertTrue(requests.getAllValues().get(0).getFilterExpression().toString()
                .contains("generation-2"));
    }

    private Document document(MarkdownDocument source, String text, String generation) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("source", source.source());
        metadata.put("document_version", source.version());
        metadata.put("chunk_order", 0);
        metadata.put("heading_path", List.of("Java"));
        metadata.put("block_type", "TEXT");
        if (generation != null) {
            metadata.put("index_generation", generation);
        }
        return Document.builder().text(text).metadata(metadata).score(0.9).build();
    }
}
