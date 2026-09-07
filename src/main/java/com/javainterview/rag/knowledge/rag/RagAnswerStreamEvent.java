package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagAnswerStreamEvent(String type, String content, boolean grounded,
                                   List<MarkdownSearchResult> sources,
                                   List<RagAnswerClaimEvidence> claimEvidence,
                                   String retrievalStrategy, boolean degraded,
                                   String degradationReason) {

    public RagAnswerStreamEvent(String type, String content, boolean grounded,
                                List<MarkdownSearchResult> sources) {
        this(type, content, grounded, sources, List.of(), "DENSE_ONLY", false, null);
    }

    public RagAnswerStreamEvent {
        Objects.requireNonNull(type, "流式事件类型不能为空");
        content = content == null ? "" : content;
        sources = List.copyOf(Objects.requireNonNull(sources, "回答来源不能为空"));
        claimEvidence = List.copyOf(Objects.requireNonNull(claimEvidence, "回答证据判断不能为空"));
        retrievalStrategy = Objects.requireNonNull(retrievalStrategy, "检索策略不能为空");
        if (degraded && (degradationReason == null || degradationReason.isBlank())) {
            throw new IllegalArgumentException("检索降级原因不能为空");
        }
        if (!degraded) {
            degradationReason = null;
        }
    }

    public static RagAnswerStreamEvent delta(String content) {
        return new RagAnswerStreamEvent("delta", content, false, List.of());
    }

    public static RagAnswerStreamEvent complete(String answer, boolean grounded,
                                                List<MarkdownSearchResult> sources) {
        return complete(answer, grounded, sources, List.of());
    }

    public static RagAnswerStreamEvent complete(String answer, boolean grounded,
                                                List<MarkdownSearchResult> sources,
                                                List<RagAnswerClaimEvidence> claimEvidence) {
        return new RagAnswerStreamEvent("complete", answer, grounded, sources, claimEvidence,
                "DENSE_ONLY", false, null);
    }

    public static RagAnswerStreamEvent complete(RagAnswerResult result) {
        return new RagAnswerStreamEvent("complete", result.answer(), result.grounded(),
                result.sources(), result.claimEvidence(), result.retrievalStrategy(),
                result.degraded(), result.degradationReason());
    }
}
