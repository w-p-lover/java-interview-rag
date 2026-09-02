package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuceneMarkdownChunkIndexTest {

    private final LuceneMarkdownChunkIndex index =
            new LuceneMarkdownChunkIndex(new ByteBuffersDirectory());

    @AfterEach
    void closeIndex() {
        index.close();
    }

    @Test
    void recallsExactJavaIdentifiersWithBm25Scores() {
        MarkdownDocument document = MarkdownDocument.create("java/concurrency.md", "并发资料");
        index.index(document, List.of(
                chunk(document, 0, "ConcurrentHashMap 的 sizeCtl 控制初始化。"),
                chunk(document, 1, "线程池通过阻塞队列承载任务。"),
                chunk(document, 2, "ConcurrentHashMap 的 treeifyBin 处理冲突。")
        ));

        List<MarkdownSearchResult> results = index.search("treeifyBin", 2);

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).chunkOrder());
        assertTrue(results.get(0).similarity() > 0.0);
        assertEquals("java/concurrency.md", results.get(0).source());
    }

    @Test
    void exposesExplainableFieldWeightsAndPrioritizesCodeIdentifiers() {
        MarkdownDocument document = MarkdownDocument.create("java/fielded.md", "字段权重");
        index.index(document, List.of(
                chunk(document, 0, "这是普通文字说明。"),
                chunk(document, 1, "if (treeifyBin(table, hash)) {}")
        ));

        List<MarkdownSearchResult> results = index.search("treeifyBin", 2);

        assertEquals("content=1.0, heading=2.0, code=2.5, java_identifier=3.0",
                LuceneMarkdownChunkIndex.FIELD_WEIGHT_POLICY);
        assertEquals(1, results.get(0).chunkOrder());
    }

    @Test
    void repeatedIndexingDoesNotDuplicateStableChunksAndSourceDeletionRemovesHits() {
        MarkdownDocument document = MarkdownDocument.create("java/map.md", "HashMap");
        List<MarkdownChunk> chunks = List.of(chunk(document, 0, "HashMap 在 Java 8 中使用 treeifyBin。"));

        index.index(document, chunks);
        index.index(document, chunks);
        assertEquals(1, index.search("treeifyBin", 5).size());

        index.deleteBySource(document.source());
        assertTrue(index.search("treeifyBin", 5).isEmpty());
    }

    @Test
    void rebuildKeepsOnlyTheCurrentVersion() {
        MarkdownDocument oldDocument = MarkdownDocument.create("java/versioned.md", "旧版本");
        MarkdownDocument newDocument = MarkdownDocument.create("java/versioned.md", "新版本");
        index.index(oldDocument, List.of(chunk(oldDocument, 0, "sizeCtl old marker")));
        index.index(newDocument, List.of(chunk(newDocument, 0, "sizeCtl new marker")));
        index.deleteOtherVersions(newDocument);

        List<MarkdownSearchResult> results = index.search("sizeCtl", 5);

        assertEquals(1, results.size());
        assertEquals(newDocument.version(), results.get(0).documentVersion());
        assertTrue(results.get(0).content().contains("new marker"));
    }

    private MarkdownChunk chunk(MarkdownDocument document, int order, String content) {
        return new MarkdownChunk(document.source(), order, order, 2,
                List.of("Java", "集合"), content);
    }
}
