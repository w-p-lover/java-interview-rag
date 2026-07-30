package com.javainterview.rag.interview.review;

import java.util.List;

public interface InterviewReviewAttemptStore {

    void create(InterviewReviewAttempt attempt);

    List<InterviewReviewAttempt> findByReviewTaskId(String sessionId, String reviewTaskId);
}
