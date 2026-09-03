package com.javainterview.rag.knowledge.rag;

import java.util.List;

record RagAnswerEvidenceValidation(
        boolean valid,
        List<RagAnswerClaimEvidence> claims
) {

    RagAnswerEvidenceValidation {
        claims = List.copyOf(claims);
    }

    boolean hasCitationInvalid() {
        return claims.stream().anyMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.CITATION_INVALID);
    }

    boolean hasConflict() {
        return claims.stream().anyMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING);
    }
}
