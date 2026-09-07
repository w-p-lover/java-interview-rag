package com.javainterview.rag.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FixedRagEvaluationDatasetTest {

    @Test
    void providesStableCasesWithExpectedSourcesAndAnswerPoints() {
        FixedRagEvaluationDataset dataset = new FixedRagEvaluationDataset();

        List<RagEvaluationCase> cases = dataset.findAll();

        assertEquals(26, cases.size());
        RagEvaluationCase hashMap = cases.get(0);
        assertEquals("java-collections-hashmap", hashMap.id());
        assertEquals("HashMap 在什么条件下树化，平均查询复杂度是多少？", hashMap.question());
        assertEquals(RagEvaluationCaseType.EXACT_TERM, hashMap.type());
        assertEquals(List.of("java/collections.md"), hashMap.expectedSources());
        assertEquals(List.of(
                List.of("Java 集合", "HashMap 的查询过程"),
                List.of("Java 集合", "链表树化条件")
        ), hashMap.expectedHeadingPaths());
        assertFalse(hashMap.expectedAnswerPoints().isEmpty());
        assertEquals(26, cases.stream().map(RagEvaluationCase::id).distinct().count());

        Map<RagEvaluationCaseType, Long> counts = cases.stream()
                .collect(Collectors.groupingBy(RagEvaluationCase::type, Collectors.counting()));
        assertEquals(Map.of(
                RagEvaluationCaseType.EXACT_TERM, 4L,
                RagEvaluationCaseType.SIMILAR_SECTION, 4L,
                RagEvaluationCaseType.HEADING_PATH, 3L,
                RagEvaluationCaseType.AMBIGUOUS, 3L,
                RagEvaluationCaseType.NO_ANSWER, 9L,
                RagEvaluationCaseType.CROSS_SECTION, 3L
        ), counts);
        assertEquals(true, cases.stream()
                .filter(item -> item.type() == RagEvaluationCaseType.NO_ANSWER)
                .allMatch(item -> item.expectedSources().isEmpty()
                        && item.expectedHeadingPaths().isEmpty()));
        assertEquals(true, cases.stream()
                .filter(item -> item.type() != RagEvaluationCaseType.NO_ANSWER)
                .allMatch(item -> !item.expectedSources().isEmpty()
                        && !item.expectedHeadingPaths().isEmpty()));
    }
}
