package com.javainterview.rag.knowledge.feedback;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!storage")
public final class NoopRagAnswerFeedbackStore implements RagAnswerFeedbackStore {
    @Override public void record(RagAnswerFeedback feedback) { }
    @Override public List<RagAnswerFeedback> findRecent(int limit, String feedback, String reviewStatus) { return List.of(); }
    @Override public Optional<RagAnswerFeedback> findById(long id) { return Optional.empty(); }
    @Override public RagAnswerFeedback review(long id, String reviewStatus, String reviewNote, String failureLayer) { throw new IllegalArgumentException("反馈不存在"); }
}
