package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagAnswerStreamEvent(String type, String content, boolean grounded,
                                   List<MarkdownSearchResult> sources,
                                   List<RagAnswerClaimEvidence> claimEvidence) {

    public RagAnswerStreamEvent(String type, String content, boolean grounded,
                                List<MarkdownSearchResult> sources) {
        this(type, content, grounded, sources, List.of());
    }

    public RagAnswerStreamEvent {
        Objects.requireNonNull(type, "流式事件类型不能为空");
        content = content == null ? "" : content;
        sources = List.copyOf(Objects.requireNonNull(sources, "回答来源不能为空"));
        claimEvidence = List.copyOf(Objects.requireNonNull(claimEvidence, "回答证据判断不能为空"));
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
        return new RagAnswerStreamEvent("complete", answer, grounded, sources, claimEvidence);
    }
}
