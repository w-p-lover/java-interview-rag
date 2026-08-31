package com.javainterview.rag.interview.session;

import java.time.LocalDateTime;

public record InterviewConversationEvent(
        String type,
        int sequence,
        int questionIndex,
        String summary,
        LocalDateTime occurredAt
) {
}
