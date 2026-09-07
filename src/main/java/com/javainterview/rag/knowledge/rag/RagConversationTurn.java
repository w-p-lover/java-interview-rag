package com.javainterview.rag.knowledge.rag;

import java.util.Objects;

public record RagConversationTurn(String question, String answer) {

    public RagConversationTurn {
        question = requireText(question, "历史问题不能为空");
        answer = requireText(answer, "历史回答不能为空");
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
