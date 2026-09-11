package com.javainterview.rag.knowledge.review;
import java.util.List;
import java.util.Optional;
public interface KnowledgeReviewAttemptStore {
    KnowledgeReviewAttempt create(KnowledgeReviewAttempt attempt);
    default KnowledgeReviewAttempt create(String idempotencyKey, KnowledgeReviewAttempt attempt) {
        return create(attempt);
    }
    default Optional<KnowledgeReviewAttempt> findByIdempotencyKey(String idempotencyKey) {
        return Optional.empty();
    }
    List<KnowledgeReviewAttempt> findByTaskId(String taskId);
}
