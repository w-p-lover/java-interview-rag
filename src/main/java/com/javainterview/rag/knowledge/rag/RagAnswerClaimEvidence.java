package com.javainterview.rag.knowledge.rag;

import java.util.List;

record RagAnswerClaimEvidence(
        String claim,
        List<Integer> sourceNumbers,
        RagAnswerClaimEvidenceStatus status,
        double anchorCoverage
) {

    RagAnswerClaimEvidence {
        sourceNumbers = List.copyOf(sourceNumbers);
    }
}
