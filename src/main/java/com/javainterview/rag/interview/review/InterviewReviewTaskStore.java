package com.javainterview.rag.interview.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewReviewTaskStore {

    void createIfAbsent(InterviewReviewTask task);

    default List<InterviewReviewTask> findBySessionId(String sessionId) {
        return findBySessionId(sessionId, null, null, null);
    }

    List<InterviewReviewTask> findBySessionId(String sessionId,
                                              InterviewReviewTaskStatus status,
                                              LocalDate plannedDateFrom,
                                              LocalDate plannedDateTo);

    default List<InterviewReviewTask> findBySessionIds(List<String> sessionIds) {
        return sessionIds.stream()
                .flatMap(sessionId -> findBySessionId(sessionId).stream())
                .toList();
    }

    Optional<InterviewReviewTask> findBySessionAndId(String sessionId, String taskId);

    Optional<InterviewReviewTask> complete(String sessionId, String taskId,
                                           LocalDateTime completedAt);

    Optional<InterviewReviewTask> schedule(String sessionId, String taskId,
                                           LocalDate plannedReviewDate);
}
