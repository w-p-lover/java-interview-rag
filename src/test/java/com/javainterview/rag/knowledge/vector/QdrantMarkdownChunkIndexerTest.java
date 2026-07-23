package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QdrantMarkdownChunkIndexerTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writesTraceableDocumentsWithStableIds() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", "# Java\n集合");
        List<MarkdownChunk> chunks = List.of(
                new MarkdownChunk("java/basic.md", 0, 0, 1, List.of("Java"), "集合"),
                new MarkdownChunk("java/basic.md", 1, 1, 2, List.of("Java", "并发"), "线程池")
        );

        indexer.index(document, chunks);
        indexer.index(document, chunks);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(2)).add(captor.capture());
        List<Document> firstWrite = captor.getAllValues().get(0);
        List<Document> secondWrite = captor.getAllValues().get(1);

        assertEquals(2, firstWrite.size());
        assertEquals(firstWrite.get(0).getId(), secondWrite.get(0).getId());
        UUID.fromString(firstWrite.get(0).getId());
        assertNotEquals(firstWrite.get(0).getId(), firstWrite.get(1).getId());
        assertEquals("集合", firstWrite.get(0).getText());
        assertEquals("java/basic.md", firstWrite.get(0).getMetadata().get("source"));
        assertEquals(document.version(), firstWrite.get(0).getMetadata().get("document_version"));
        assertEquals(0, firstWrite.get(0).getMetadata().get("chunk_order"));
        assertEquals(0, firstWrite.get(0).getMetadata().get("section_order"));
        assertEquals(1, firstWrite.get(0).getMetadata().get("heading_level"));
        assertEquals(List.of("Java"), firstWrite.get(0).getMetadata().get("heading_path"));
    }

    @Test
    void deletesOtherVersionsForTheSameSourceWithAStructuredFilter() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", "# Java\n集合");

        indexer.deleteOtherVersions(document);

        ArgumentCaptor<Filter.Expression> captor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(captor.capture());
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        assertEquals(builder.and(
                builder.eq("source", document.source()),
                builder.ne("document_version", document.version())
        ).build(), captor.getValue());
    }

    @Test
    void deletesAllVectorsForTheSameSourceWithAStructuredFilter() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);

        indexer.deleteBySource("java/basic.md");

        ArgumentCaptor<Filter.Expression> captor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(captor.capture());
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        assertEquals(builder.eq("source", "java/basic.md").build(), captor.getValue());
    }
}
