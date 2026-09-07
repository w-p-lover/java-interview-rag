package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownHeadingParser;
import com.javainterview.rag.knowledge.document.MarkdownSectionChunker;
import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCaseType;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                "线程池接收新任务后先创建核心线程，随后把任务放入工作队列。")));

        List<MarkdownSearchResult> results = service.search("线程池如何接收新任务？", 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void rejectsPureChineseNearDomainQuestionsWithOnlyPartialConceptOverlap() {
        MarkdownDocument concurrency = MarkdownDocument.create("java/concurrency.md", "并发资料");
        MarkdownDocument indexes = MarkdownDocument.create("mysql/indexes.md", "索引资料");
        MarkdownDocument jvm = MarkdownDocument.create("java/jvm.md", "JVM 资料");
        index.index(concurrency, List.of(chunk(concurrency, 0,
                "线程池接收任务后先创建核心线程，再放入工作队列，队列满后执行拒绝策略。")));
        index.index(indexes, List.of(chunk(indexes, 0,
                "联合索引按照列顺序组织，查询通常应从最左列开始连续匹配。")));
        index.index(jvm, List.of(chunk(jvm, 0,
                "垃圾收集器从 GC Roots 开始进行可达性分析，不可达对象进入回收判定。")));

        assertFalse(index.search("线程池如何实现定时任务调度？", 5).isEmpty());
        assertFalse(index.search("联合索引能否替代全文索引？", 5).isEmpty());
        assertFalse(index.search("垃圾收集器如何调优停顿时间？", 5).isEmpty());
        assertTrue(service.search("线程池如何实现定时任务调度？", 5).isEmpty());
        assertTrue(service.search("联合索引能否替代全文索引？", 5).isEmpty());
        assertTrue(service.search("垃圾收集器如何调优停顿时间？", 5).isEmpty());
    }

    @Test
    void keepsChineseCandidatesWithoutCuratedUnsupportedPhrase() {
        MarkdownDocument document = MarkdownDocument.create("rag/rag-basics.md", "RAG 资料");
        index.index(document, List.of(chunk(document, 0,
                "topK 控制候选片段数量，相似度阈值过滤相关性不足的资料，候选过多会引入上下文噪声。")));

        List<MarkdownSearchResult> results = service.search(
                "检索候选太多导致上下文混入不相关内容，应由哪两个参数先控制数量和可靠性？", 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void keepsAnswerableCasesAndRejectsNoAnswerCasesInFixedCorpus() throws IOException {
        List<String> sources = List.of(
                "java/collections.md", "java/concurrent-collections.md",
                "java/concurrency.md", "java/jvm.md", "mysql/indexes.md",
                "mysql/transactions.md", "rag/agent-basics.md", "rag/rag-basics.md");
        MarkdownHeadingParser parser = new MarkdownHeadingParser();
        MarkdownSectionChunker chunker = new MarkdownSectionChunker();
        for (String source : sources) {
            String content = Files.readString(Path.of("evaluation", "corpus", source));
            MarkdownDocument document = MarkdownDocument.create(source, content);
            index.index(document, chunker.chunk(parser.parse(document), 800));
        }

        for (var evaluationCase : new FixedRagEvaluationDataset().findAll()) {
            List<MarkdownSearchResult> results = service.search(evaluationCase.question(), 5);
            if (evaluationCase.type() == RagEvaluationCaseType.NO_ANSWER) {
                assertTrue(results.isEmpty(), evaluationCase.id() + " 应拒答");
            } else {
                assertFalse(results.isEmpty(), evaluationCase.id() + " 不应被误拒答");
            }
        }
    }

    @Test
    void exposesVersionedEvidencePolicyCode() {
        assertEquals("technical_anchor_coverage_v2", SparseQueryEvidencePolicy.CODE);
    }

    private MarkdownChunk chunk(MarkdownDocument document, int order, String content) {
        return new MarkdownChunk(document.source(), order, order, 2,
                List.of("知识资料", "检索样本"), content);
    }
}
