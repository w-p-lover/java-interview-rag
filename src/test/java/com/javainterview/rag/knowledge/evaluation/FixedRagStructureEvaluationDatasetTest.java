package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedRagStructureEvaluationDatasetTest {

    @Test
    void providesStablePdfStructureCases() {
        List<RagEvaluationCase> cases = new FixedRagStructureEvaluationDataset().findAll();

        assertEquals(3, cases.size());
        assertEquals(3, cases.stream().map(RagEvaluationCase::id).distinct().count());
        assertTrue(cases.stream().allMatch(item -> item.type() == RagEvaluationCaseType.DOCUMENT_STRUCTURE));
        assertTrue(cases.stream().allMatch(item -> item.expectedStructures().size() == 1));
        assertEquals(List.of(KnowledgeBlockType.CODE, KnowledgeBlockType.TABLE, KnowledgeBlockType.TEXT),
                cases.stream()
                        .map(item -> item.expectedStructures().get(0).blockType())
                        .toList());
        assertEquals(List.of(1, 1, 2), cases.stream()
                .map(item -> item.expectedStructures().get(0).pageNumber())
                .toList());
    }
}
