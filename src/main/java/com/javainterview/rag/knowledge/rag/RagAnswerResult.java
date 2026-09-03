package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources,
                              List<RagAnswerClaimEvidence> claimEvidence) {

    public RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources) {
        this(answer, grounded, sources, List.of());
    }

    public RagAnswerResult {
        Objects.requireNonNull(answer, "回答不能为空");
        sources = List.copyOf(Objects.requireNonNull(sources, "来源不能为空"));
        claimEvidence = List.copyOf(Objects.requireNonNull(claimEvidence, "回答证据判断不能为空"));
    }
}
