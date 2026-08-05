package com.javainterview.rag.interview.tool;

public record InterviewReviewAssistantResult(String requestId, String answer) {

    public InterviewReviewAssistantResult {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("助手请求标识不能为空");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("复习助手回答不能为空");
        }
        requestId = requestId.strip();
        answer = answer.strip();
    }
}
