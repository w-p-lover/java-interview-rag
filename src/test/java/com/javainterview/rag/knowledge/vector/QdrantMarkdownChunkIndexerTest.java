package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        assertEquals("MARKDOWN", firstWrite.get(0).getMetadata().get("document_format"));
        assertEquals("TEXT", firstWrite.get(0).getMetadata().get("block_type"));
        assertEquals("text-embedding-v3", firstWrite.get(0).getMetadata().get("embedding_model"));
        assertEquals(1024, firstWrite.get(0).getMetadata().get("embedding_dimensions"));
        assertEquals("markdown-section-chunker-v3",
                firstWrite.get(0).getMetadata().get("chunk_policy_version"));
        assertEquals("pdf-knowledge-parser-v3",
                firstWrite.get(0).getMetadata().get("parser_version"));
        assertEquals("knowledge-index-v2",
                firstWrite.get(0).getMetadata().get("index_schema_version"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void preservesPdfFormatAndPageMetadataForKnowledgeChunks() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/collections-guide", "# 第 3 页\nHashMap");

        indexer.index(document, List.of(new MarkdownChunk(document.source(), 0, 0, 1,
                List.of("HashMap"), "HashMap", 3, KnowledgeBlockType.CODE)));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document stored = (Document) captor.getValue().get(0);
        assertEquals("PDF", stored.getMetadata().get("document_format"));
        assertEquals(3, stored.getMetadata().get("page_number"));
        assertEquals("CODE", stored.getMetadata().get("block_type"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writesAtMostTenDocumentsPerVectorStoreCall() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/large.md", "大文档");
        List<MarkdownChunk> chunks = IntStream.range(0, 26)
                .mapToObj(order -> new MarkdownChunk(document.source(), order, order,
                        1, List.of("Java"), "内容" + order))
                .toList();

        indexer.index(document, chunks);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(3)).add(captor.capture());
        assertEquals(List.of(10, 10, 6),
                captor.getAllValues().stream().map(List::size).toList());
        assertEquals(0, ((Document) captor.getAllValues().get(0).get(0))
                .getMetadata().get("chunk_order"));
        assertEquals(25, ((Document) captor.getAllValues().get(2).get(5))
                .getMetadata().get("chunk_order"));
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
    void deletesOnlyTheFailedDocumentVersionWithAStructuredFilter() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", "# Java\n集合");

        indexer.deleteVersion(document);

        ArgumentCaptor<Filter.Expression> captor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(captor.capture());
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        assertEquals(builder.and(
                builder.eq("source", document.source()),
                builder.eq("document_version", document.version())
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

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void rebuildsExpectedChunksBeforeRemovingOtherVersionsAndStaleChunkOrders() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create(
                "java/rebuild.md", "# Java\n集合\n\n并发");
        List<MarkdownChunk> chunks = List.of(
                new MarkdownChunk(document.source(), 0, 0, 1, List.of("Java"), "集合"),
                new MarkdownChunk(document.source(), 1, 0, 1, List.of("Java"), "并发")
        );

        indexer.rebuild(document, chunks);

        var orderedCalls = inOrder(vectorStore);
        orderedCalls.verify(vectorStore).add(anyList());
        ArgumentCaptor<Filter.Expression> filters = ArgumentCaptor.forClass(Filter.Expression.class);
        orderedCalls.verify(vectorStore, times(2)).delete(filters.capture());
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        assertEquals(builder.and(
                builder.eq("source", document.source()),
                builder.ne("document_version", document.version())
        ).build(), filters.getAllValues().get(0));
        assertEquals(builder.and(
                builder.and(
                        builder.eq("source", document.source()),
                        builder.eq("document_version", document.version())
                ),
                builder.gte("chunk_order", chunks.size())
        ).build(), filters.getAllValues().get(1));
    }

    @Test
    void rebuildsAnEmptyDocumentByDeletingTheWholeSource() {
        VectorStore vectorStore = mock(VectorStore.class);
        QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
        MarkdownDocument document = MarkdownDocument.create("java/empty.md", "");

        indexer.rebuild(document, List.of());

        verify(vectorStore, never()).add(anyList());
        ArgumentCaptor<Filter.Expression> filter = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filter.capture());
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        assertEquals(builder.eq("source", document.source()).build(), filter.getValue());
    }
}
