package com.javainterview.rag.knowledge.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KnowledgeReviewTaskStore {
    KnowledgeReviewTask createIfAbsent(KnowledgeReviewTask task);

    List<KnowledgeReviewTask> find(KnowledgeReviewTaskStatus status,
                                   LocalDate plannedDateFrom,
                                   LocalDate plannedDateTo);

    Optional<KnowledgeReviewTask> complete(String taskId, LocalDateTime completedAt);

    Optional<KnowledgeReviewTask> schedule(String taskId, LocalDate plannedReviewDate);
}
