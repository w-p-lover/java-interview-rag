package com.javainterview.rag.knowledge.feedback;

import java.util.List;

public interface RagAnswerFeedbackStore {
    void record(RagAnswerFeedback feedback);

    List<RagAnswerFeedback> findRecent(int limit, String feedback, String reviewStatus);

    RagAnswerFeedback review(long id, String reviewStatus, String reviewNote);
}
