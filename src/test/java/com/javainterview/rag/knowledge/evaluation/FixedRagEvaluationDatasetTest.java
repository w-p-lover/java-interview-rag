package com.javainterview.rag.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FixedRagEvaluationDatasetTest {

    @Test
    void providesStableCasesWithExpectedSourcesAndAnswerPoints() {
        FixedRagEvaluationDataset dataset = new FixedRagEvaluationDataset();

        List<RagEvaluationCase> cases = dataset.findAll();

        assertEquals(6, cases.size());
        RagEvaluationCase hashMap = cases.get(0);
        assertEquals("java-collections-hashmap", hashMap.id());
        assertEquals("HashMap 在什么条件下树化，平均查询复杂度是多少？", hashMap.question());
        assertEquals(List.of("java/collections.md"), hashMap.expectedSources());
        assertFalse(hashMap.expectedAnswerPoints().isEmpty());
        assertEquals(6, cases.stream().map(RagEvaluationCase::id).distinct().count());
    }
}
