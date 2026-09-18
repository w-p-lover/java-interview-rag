package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.bank.QuestionBankContext;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Objects;

public record InterviewQuestion(
        String question,
        String objective,
        List<InterviewFocusSource> basisSources,
        String knowledgeQuery,
        boolean knowledgeGrounded,
        List<MarkdownSearchResult> knowledgeSources,
        QuestionBankContext bankContext
) {

    public InterviewQuestion(String question, String objective, List<InterviewFocusSource> basisSources,
                              String knowledgeQuery, boolean knowledgeGrounded,
                              List<MarkdownSearchResult> knowledgeSources) {
        this(question, objective, basisSources, knowledgeQuery, knowledgeGrounded, knowledgeSources, null);
    }

    public InterviewQuestion {
        question = requireText(question, "面试题不能为空");
        objective = requireText(objective, "考察目标不能为空");
        basisSources = List.copyOf(Objects.requireNonNull(basisSources, "面试题依据不能为空"));
        if (basisSources.isEmpty()) {
            throw new IllegalArgumentException("面试题依据不能为空");
        }
        knowledgeQuery = requireText(knowledgeQuery, "知识检索查询不能为空");
        knowledgeSources = List.copyOf(Objects.requireNonNull(knowledgeSources, "知识来源不能为空"));
        if (knowledgeGrounded != !knowledgeSources.isEmpty()) {
            throw new IllegalArgumentException("知识来源状态与实际来源不一致");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
