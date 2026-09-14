package com.javainterview.rag.knowledge.feedback;

import java.time.LocalDateTime;

public record RagAnswerFeedback(String questionHash, boolean grounded, String retrievalStrategy,
                                String feedback, LocalDateTime createdAt) {
    public RagAnswerFeedback {
        if (questionHash == null || questionHash.isBlank()) throw new IllegalArgumentException("问题标识不能为空");
        if (!"HELPFUL".equals(feedback) && !"NOT_HELPFUL".equals(feedback)) {
            throw new IllegalArgumentException("反馈类型不合法");
        }
    }
}
