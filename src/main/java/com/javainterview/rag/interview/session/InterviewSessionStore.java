package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.question.HistoricalInterviewQuestion;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionStore {

    void create(InterviewSession session);

    default Optional<InterviewSession> createIfAbsent(InterviewSession session, String requestId) {
        create(session);
        return Optional.empty();
    }

    Optional<InterviewSession> findById(String sessionId);

    default Optional<InterviewSession> findByRequestId(String requestId) {
        return Optional.empty();
    }

    default List<HistoricalInterviewQuestion> findRecentQuestions(InterviewSkill skill, int limit) {
        return List.of();
    }

    boolean update(InterviewSession session, long expectedVersion);
}
