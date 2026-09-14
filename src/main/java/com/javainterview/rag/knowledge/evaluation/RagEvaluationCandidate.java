package com.javainterview.rag.knowledge.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record RagEvaluationCandidate(
        String id,
        Long feedbackId,
        String question,
        List<String> expectedSources,
        List<String> expectedAnswerPoints,
        RagEvaluationCandidateStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public RagEvaluationCandidate {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("评估候选 ID 不能为空");
        if (question == null || question.isBlank()) throw new IllegalArgumentException("评估问题不能为空");
        expectedSources = List.copyOf(expectedSources == null ? List.of() : expectedSources);
        expectedAnswerPoints = List.copyOf(expectedAnswerPoints == null ? List.of() : expectedAnswerPoints);
        if (expectedAnswerPoints.isEmpty()) throw new IllegalArgumentException("评估候选至少需要一个预期回答要点");
        if (status == null) throw new IllegalArgumentException("评估候选状态不能为空");
        if (createdAt == null) throw new IllegalArgumentException("创建时间不能为空");
    }
}
