package com.javainterview.rag.knowledge.rag;

import java.util.List;

record RagAnswerCitationValidation(
        boolean valid,
        List<Integer> citedSourceNumbers,
        List<Integer> invalidSourceNumbers,
        List<RagAnswerClaimCitation> claims
) {

    RagAnswerCitationValidation {
        citedSourceNumbers = List.copyOf(citedSourceNumbers);
        invalidSourceNumbers = List.copyOf(invalidSourceNumbers);
        claims = List.copyOf(claims);
    }
}
