package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Optional;

public interface RagEvaluationCandidateStore {
    RagEvaluationCandidate createIfAbsent(RagEvaluationCandidate candidate);
    List<RagEvaluationCandidate> findRecent(int limit, RagEvaluationCandidateStatus status);
    Optional<RagEvaluationCandidate> findById(String id);
    RagEvaluationCandidate review(String id, RagEvaluationCandidateStatus status);
}
