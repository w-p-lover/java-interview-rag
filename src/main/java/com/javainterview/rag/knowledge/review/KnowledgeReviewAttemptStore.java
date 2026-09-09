package com.javainterview.rag.knowledge.review;
import java.util.List;
public interface KnowledgeReviewAttemptStore {
    KnowledgeReviewAttempt create(KnowledgeReviewAttempt attempt);
    List<KnowledgeReviewAttempt> findByTaskId(String taskId);
}
