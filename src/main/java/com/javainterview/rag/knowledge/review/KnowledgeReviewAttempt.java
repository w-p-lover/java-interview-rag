package com.javainterview.rag.knowledge.review;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import java.time.LocalDateTime;
import java.util.Objects;

public record KnowledgeReviewAttempt(String id, String taskId, String answer,
                                     InterviewAnswerEvaluation evaluation,
                                     LocalDateTime createdAt) {
    public KnowledgeReviewAttempt {
        if (id == null || id.isBlank() || taskId == null || taskId.isBlank()
                || answer == null || answer.isBlank()) throw new IllegalArgumentException("复习回答不能为空");
        evaluation = Objects.requireNonNull(evaluation, "复习评价不能为空");
        createdAt = Objects.requireNonNull(createdAt, "复习回答时间不能为空");
    }
}
