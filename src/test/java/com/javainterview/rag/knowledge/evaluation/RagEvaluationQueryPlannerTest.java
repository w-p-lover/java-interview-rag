package com.javainterview.rag.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationQueryPlannerTest {

    private final RagEvaluationQueryPlanner planner = new RagEvaluationQueryPlanner();

    @Test
    void keepsSimpleQuestionAsSingleQuery() {
        assertEquals(List.of("JVM 如何通过 GC Roots 判断对象是否可回收？"),
                planner.plan("JVM 如何通过 GC Roots 判断对象是否可回收？"));
    }

    @Test
    void splitsMultipleSentencesAndClausesWithoutExceedingLimit() {
        List<String> queries = planner.plan(
                "Map 查询慢一定是时间复杂度变了吗？应检查哪些与冲突和容量有关的因素？还要核对初始化容量，避免无关猜测。");

        assertEquals("Map 查询慢一定是时间复杂度变了吗？应检查哪些与冲突和容量有关的因素？还要核对初始化容量，避免无关猜测。",
                queries.get(0));
        assertTrue(queries.contains("Map 查询慢一定是时间复杂度变了吗"));
        assertTrue(queries.contains("应检查哪些与冲突和容量有关的因素"));
        assertEquals(4, queries.size());
    }

    @Test
    void splitsCoordinatedQuestionWhenSeparateEvidenceIsRequested() {
        List<String> queries = planner.plan("单线程映射与高并发映射应如何选择，并分别关注哪些查询和竞争成本？");

        assertTrue(queries.contains("单线程映射"));
        assertTrue(queries.contains("高并发映射应如何选择，并分别关注哪些查询和竞争成本"));
        assertTrue(queries.contains("单线程映射与高并发映射应如何选择"));
    }

    @Test
    void rejectsBlankQuestion() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> planner.plan("  "));

        assertEquals("评估问题不能为空", exception.getMessage());
    }

    @Test
    void keepsFixedDatasetQueryCostExplicit() {
        List<List<String>> plans = new FixedRagEvaluationDataset().findAll().stream()
                .map(evaluationCase -> planner.plan(evaluationCase.question()))
                .toList();

        assertEquals(13, plans.stream().filter(queries -> queries.size() > 1).count());
        assertEquals(29, plans.stream().mapToInt(queries -> queries.size() - 1).sum());
    }
}
