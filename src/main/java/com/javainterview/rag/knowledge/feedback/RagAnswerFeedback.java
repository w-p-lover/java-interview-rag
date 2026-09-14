package com.javainterview.rag.knowledge.feedback;

import java.time.LocalDateTime;

public record RagAnswerFeedback(Long id, String questionHash, boolean grounded, String retrievalStrategy,
                                String feedback, String reviewStatus, String reviewNote,
                                LocalDateTime createdAt, LocalDateTime reviewedAt) {
    public RagAnswerFeedback {
        if (questionHash == null || questionHash.isBlank()) throw new IllegalArgumentException("问题标识不能为空");
        if (!"HELPFUL".equals(feedback) && !"NOT_HELPFUL".equals(feedback)) {
            throw new IllegalArgumentException("反馈类型不合法");
        }
        if (!"PENDING".equals(reviewStatus) && !"EVALUATION".equals(reviewStatus)
                && !"IGNORED".equals(reviewStatus)) {
            throw new IllegalArgumentException("反馈审阅状态不合法");
        }
    }
}
