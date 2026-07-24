package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;

public record RagAnswerResponse(String answer, boolean grounded, List<MarkdownSearchResult> sources) {

    public static RagAnswerResponse from(RagAnswerResult result) {
        return new RagAnswerResponse(result.answer(), result.grounded(), result.sources());
    }
}
