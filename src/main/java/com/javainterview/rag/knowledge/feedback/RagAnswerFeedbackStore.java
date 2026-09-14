package com.javainterview.rag.knowledge.feedback;

import java.util.List;
import java.util.Optional;

public interface RagAnswerFeedbackStore {
    void record(RagAnswerFeedback feedback);

    List<RagAnswerFeedback> findRecent(int limit, String feedback, String reviewStatus);

    Optional<RagAnswerFeedback> findById(long id);

    RagAnswerFeedback review(long id, String reviewStatus, String reviewNote, String failureLayer);
}
