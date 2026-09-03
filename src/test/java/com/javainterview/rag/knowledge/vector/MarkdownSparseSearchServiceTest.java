package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownSparseSearchServiceTest {

    private final LuceneMarkdownChunkIndex index =
            new LuceneMarkdownChunkIndex(new ByteBuffersDirectory());
    private final MarkdownSparseSearchService service = new MarkdownSparseSearchService(index);

    @AfterEach
    void closeIndex() {
        index.close();
    }

    @Test
    void keepsCandidatesWhenTechnicalAnchorsHaveEnoughCoverage() {
        MarkdownDocument document = MarkdownDocument.create("java/collections.md", "集合资料");
        index.index(document, List.of(chunk(document, 0,
                "HashMap 根据 hashCode 定位数组桶，平均查询复杂度为 O(1)。")));

        List<MarkdownSearchResult> results = service.search(
                "HashMap 查询慢时应检查 hashCode 吗？", 5);

        assertEquals(1, results.size());
        assertEquals("java/collections.md", results.get(0).source());
    }

    @Test
    void rejectsLowRelatedCandidatesThatMissMostTechnicalAnchors() {
        MarkdownDocument document = MarkdownDocument.create("mysql/transactions.md", "事务资料");
        index.index(document, List.of(chunk(document, 0,
                "InnoDB 使用 Read View 与 undo log 提供 MVCC 一致性读取。")));

        List<MarkdownSearchResult> results = service.search(
                "InnoDB 的 next-key lock 和 gap lock 在什么情况下生效？", 5);

        assertTrue(results.isEmpty());
    }

    @Test
    void retainsCandidatePresenceBehaviorForQueriesWithoutTechnicalAnchors() {
        MarkdownDocument document = MarkdownDocument.create("java/concurrency.md", "并发资料");
        index.index(document, List.of(chunk(document, 0,
                "线程池先创建核心线程，随后把任务放入工作队列。")));

        List<MarkdownSearchResult> results = service.search("线程池如何接收新任务？", 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void exposesVersionedEvidencePolicyCode() {
        assertEquals("technical_anchor_coverage_v1", SparseQueryEvidencePolicy.CODE);
    }

    private MarkdownChunk chunk(MarkdownDocument document, int order, String content) {
        return new MarkdownChunk(document.source(), order, order, 2,
                List.of("知识资料", "检索样本"), content);
    }
}
