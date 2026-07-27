package com.javainterview.rag.interview.session;

import java.time.LocalDateTime;
import java.util.Objects;

public record InterviewSessionAnswer(
        int questionIndex,
        String answer,
        LocalDateTime answeredAt
) {

    public InterviewSessionAnswer {
        if (questionIndex < 0) {
            throw new IllegalArgumentException("回答题号不能为负数");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("回答不能为空");
        }
        answeredAt = Objects.requireNonNull(answeredAt, "回答时间不能为空");
    }
}
