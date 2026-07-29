package com.javainterview.rag.interview.review;

import java.util.List;

public interface InterviewReviewTaskStore {

    void createIfAbsent(InterviewReviewTask task);

    List<InterviewReviewTask> findBySessionId(String sessionId);
}
