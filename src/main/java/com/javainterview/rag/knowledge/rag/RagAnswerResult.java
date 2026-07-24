package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record RagAnswerResult(String answer, boolean grounded, List<MarkdownSearchResult> sources) {

    public RagAnswerResult {
        Objects.requireNonNull(answer, "回答不能为空");
        sources = List.copyOf(Objects.requireNonNull(sources, "来源不能为空"));
    }
}
