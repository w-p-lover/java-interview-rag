package com.javainterview.rag.knowledge.rag;

import java.util.List;
import java.util.Objects;

public record RagAnswerClaimEvidence(
        String claim,
        List<Integer> sourceNumbers,
        RagAnswerClaimEvidenceStatus status,
        double anchorCoverage
) {

    public RagAnswerClaimEvidence(String claim, List<Integer> sourceNumbers,
                                  RagAnswerClaimEvidenceStatus status, double anchorCoverage) {
        this.claim = Objects.requireNonNull(claim, "回答断言不能为空");
        this.sourceNumbers = List.copyOf(Objects.requireNonNull(sourceNumbers, "引用资料不能为空"));
        this.status = Objects.requireNonNull(status, "断言证据状态不能为空");
        this.anchorCoverage = anchorCoverage;
    }
}
