package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources,
                              List<RagAnswerClaimEvidence> claimEvidence,
                              String retrievalStrategy, boolean degraded,
                              String degradationReason) {

    public RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources) {
        this(answer, grounded, sources, List.of(), "DENSE_ONLY", false, null);
    }

    public RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources,
                           List<RagAnswerClaimEvidence> claimEvidence) {
        this(answer, grounded, sources, claimEvidence, "DENSE_ONLY", false, null);
    }

    public RagAnswerResult {
        Objects.requireNonNull(answer, "回答不能为空");
        sources = List.copyOf(Objects.requireNonNull(sources, "来源不能为空"));
        claimEvidence = List.copyOf(Objects.requireNonNull(claimEvidence, "回答证据判断不能为空"));
        retrievalStrategy = Objects.requireNonNull(retrievalStrategy, "检索策略不能为空");
        if (degraded && (degradationReason == null || degradationReason.isBlank())) {
            throw new IllegalArgumentException("检索降级原因不能为空");
        }
        if (!degraded) {
            degradationReason = null;
        }
    }
}
