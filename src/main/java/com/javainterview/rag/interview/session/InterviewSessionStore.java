package com.javainterview.rag.interview.session;

import java.util.Optional;

public interface InterviewSessionStore {

    void create(InterviewSession session);

    Optional<InterviewSession> findById(String sessionId);

    boolean update(InterviewSession session, long expectedVersion);
}
