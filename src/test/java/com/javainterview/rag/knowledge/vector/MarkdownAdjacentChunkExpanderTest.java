package com.javainterview.rag.knowledge.vector;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkdownAdjacentChunkExpanderTest {

    @Test
    void expandsOnlySameSourceAndVersionAndDeduplicatesTheSeed() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0);
            if (request.getFilterExpression().toString().contains("java/collections.md")) {
                return List.of(document("java/collections.md", "version-1", 0, "前一段"),
                        document("java/collections.md", "version-1", 1, "当前段"),
                        document("java/collections.md", "version-2", 2, "错误版本"));
            }
            return List.of();
        });

        MarkdownSearchResult seed = result("java/collections.md", "version-1", 1, "当前段");
        List<MarkdownSearchResult> expanded = new MarkdownAdjacentChunkExpander(vectorStore)
                .expand(List.of(seed, seed));

        assertEquals(List.of(seed, new MarkdownSearchResult(
                "前一段", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.55)), expanded);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void doesNotQueryVectorStoreWhenThereIsNoReliableInitialHit() {
        VectorStore vectorStore = mock(VectorStore.class);

        List<MarkdownSearchResult> expanded = new MarkdownAdjacentChunkExpander(vectorStore).expand(List.of());

        assertEquals(List.of(), expanded);
        org.mockito.Mockito.verifyNoInteractions(vectorStore);
    }

    private Document document(String source, String version, int order, String content) {
        return Document.builder()
                .text(content)
                .metadata(Map.of(
                        "source", source,
                        "document_version", version,
                        "chunk_order", order,
                        "heading_path", List.of("Java", "集合")
                ))
                .score(0.55)
                .build();
    }

    private MarkdownSearchResult result(String source, String version, int order, String content) {
        return new MarkdownSearchResult(content, source, version, order, List.of("Java", "集合"), 0.9);
    }
}
