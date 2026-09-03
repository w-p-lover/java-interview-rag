package com.javainterview.rag.knowledge.rag;

import java.util.List;
import java.util.Objects;

public record RagAnswerEvidenceValidation(
        boolean valid,
        List<RagAnswerClaimEvidence> claims
) {

    public RagAnswerEvidenceValidation(boolean valid, List<RagAnswerClaimEvidence> claims) {
        this.valid = valid;
        this.claims = List.copyOf(Objects.requireNonNull(claims, "回答证据判断不能为空"));
    }

    boolean hasCitationInvalid() {
        return claims.stream().anyMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.CITATION_INVALID);
    }

    boolean hasConflict() {
        return claims.stream().anyMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING);
    }
}
