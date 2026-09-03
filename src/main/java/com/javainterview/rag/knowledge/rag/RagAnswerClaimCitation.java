package com.javainterview.rag.knowledge.rag;

import java.util.List;

record RagAnswerClaimCitation(
        String claim,
        List<Integer> sourceNumbers,
        RagAnswerClaimCitationStatus status
) {

    RagAnswerClaimCitation {
        sourceNumbers = List.copyOf(sourceNumbers);
    }
}
