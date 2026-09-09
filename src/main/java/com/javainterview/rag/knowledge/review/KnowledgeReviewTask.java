package com.javainterview.rag.knowledge.review;

import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record KnowledgeReviewTask(
        String id,
        String question,
        String claim,
        RagAnswerClaimEvidenceStatus evidenceStatus,
        List<Integer> sourceNumbers,
        KnowledgeReviewTaskStatus status,
        LocalDate plannedReviewDate,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public KnowledgeReviewTask {
        id = requireText(id, "知识复习任务 ID 不能为空");
        question = requireText(question, "知识问题不能为空");
        claim = requireText(claim, "待复习断言不能为空");
        evidenceStatus = Objects.requireNonNull(evidenceStatus, "证据状态不能为空");
        sourceNumbers = List.copyOf(Objects.requireNonNull(sourceNumbers, "引用资料不能为空"));
        if (sourceNumbers.stream().anyMatch(number -> number == null || number < 1)) {
            throw new IllegalArgumentException("引用资料编号必须从 1 开始");
        }
        status = Objects.requireNonNull(status, "复习任务状态不能为空");
        createdAt = Objects.requireNonNull(createdAt, "创建时间不能为空");
        if (status == KnowledgeReviewTaskStatus.PENDING && completedAt != null) {
            throw new IllegalArgumentException("待复习任务不能包含完成时间");
        }
        if (status == KnowledgeReviewTaskStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException("已完成任务必须包含完成时间");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
