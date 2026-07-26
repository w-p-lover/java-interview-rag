package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagAnswerEvaluationServiceTest {

    @Test
    void evaluatesNormalizedAnswerPointsAndActualSources() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        RagAnswerService answerService = mock(RagAnswerService.class);
        RagEvaluationCase hashMapCase = new RagEvaluationCase(
                "hashmap", "HashMap 问题", List.of("java/collections.md"),
                List.of("平均查询复杂度为 O(1)", "链表长度达到 8", "数组容量至少为 64"));
        RagEvaluationCase volatileCase = new RagEvaluationCase(
                "volatile", "volatile 问题", List.of("java/concurrency.md"),
                List.of("保证可见性", "多线程自增需要原子类或锁"));
        when(dataset.findAll()).thenReturn(List.of(hashMapCase, volatileCase));
        when(answerService.answer("HashMap 问题")).thenReturn(new RagAnswerResult(
                "常见平均查询复杂度为 o ( 1 )；链表长度达到 8。", true,
                List.of(source("java/collections.md"))));
        when(answerService.answer("volatile 问题")).thenReturn(new RagAnswerResult(
                "知识库中没有足够可靠的资料回答这个问题。", false, List.of()));
        RagAnswerEvaluationService service = new RagAnswerEvaluationService(dataset, answerService);

        RagAnswerEvaluationReport report = service.evaluate();

        assertEquals("normalized-literal-containment-v1", report.matchingMethod());
        assertEquals(2, report.totalCaseCount());
        assertEquals(1, report.groundedCaseCount());
        assertEquals(1, report.expectedSourceHitCount());
        assertEquals(5, report.totalAnswerPointCount());
        assertEquals(2, report.matchedAnswerPointCount());
        assertEquals(0.4, report.answerPointCoverageRate());

        RagAnswerEvaluationCaseResult first = report.cases().get(0);
        assertTrue(first.expectedSourceMatched());
        assertEquals(List.of("平均查询复杂度为 O(1)", "链表长度达到 8"), first.matchedAnswerPoints());
        assertEquals(List.of("数组容量至少为 64"), first.unmatchedAnswerPoints());
        assertEquals(2.0 / 3.0, first.answerPointCoverageRate());
        assertEquals("java/collections.md", first.actualSources().get(0).source());

        RagAnswerEvaluationCaseResult second = report.cases().get(1);
        assertFalse(second.grounded());
        assertFalse(second.expectedSourceMatched());
        assertEquals(List.of(), second.matchedAnswerPoints());
        assertEquals(0.0, second.answerPointCoverageRate());
    }

    private MarkdownSearchResult source(String source) {
        return new MarkdownSearchResult("内容", source, "version-1", 0,
                List.of("标题"), 0.91);
    }
}
