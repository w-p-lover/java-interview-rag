package com.javainterview.rag.interview.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewReviewTaskStore {

    void createIfAbsent(InterviewReviewTask task);

    List<InterviewReviewTask> findBySessionId(String sessionId);

    Optional<InterviewReviewTask> findBySessionAndId(String sessionId, String taskId);

    Optional<InterviewReviewTask> complete(String sessionId, String taskId,
                                           LocalDateTime completedAt);
}
