package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record InterviewReviewAttempt(
        String id,
        String sessionId,
        String reviewTaskId,
        int questionIndex,
        int attemptNumber,
        String answer,
        InterviewAnswerEvaluation evaluation,
        boolean knowledgeGrounded,
        List<InterviewReviewKnowledgeSource> knowledgeSources,
        String promptVersion,
        String rubricVersion,
        String modelName,
        int originalOverallScore,
        Integer previousOverallScore,
        int scoreDeltaFromOriginal,
        Integer scoreDeltaFromPrevious,
        LocalDateTime createdAt
) {

    public InterviewReviewAttempt {
        id = requireText(id, "复习回答 ID 不能为空");
        sessionId = requireText(sessionId, "面试会话 ID 不能为空");
        reviewTaskId = requireText(reviewTaskId, "复习任务 ID 不能为空");
        if (questionIndex < 0) {
            throw new IllegalArgumentException("复习回答题号不能为负数");
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("复习回答序号必须从 1 开始");
        }
        answer = requireText(answer, "复习回答不能为空");
        evaluation = Objects.requireNonNull(evaluation, "复习回答评价不能为空");
        knowledgeSources = List.copyOf(Objects.requireNonNull(knowledgeSources, "知识来源不能为空"));
        if (knowledgeGrounded != !knowledgeSources.isEmpty()) {
            throw new IllegalArgumentException("知识来源状态与实际来源不一致");
        }
        promptVersion = requireText(promptVersion, "评价提示词版本不能为空");
        rubricVersion = requireText(rubricVersion, "评分规则版本不能为空");
        modelName = requireText(modelName, "评价模型名称不能为空");
        if (originalOverallScore < 0 || originalOverallScore > 100) {
            throw new IllegalArgumentException("原回答总分必须在 0 到 100 之间");
        }
        if (previousOverallScore != null && (previousOverallScore < 0 || previousOverallScore > 100)) {
            throw new IllegalArgumentException("上次回答总分必须在 0 到 100 之间");
        }
        if (scoreDeltaFromOriginal != evaluation.overallScore() - originalOverallScore) {
            throw new IllegalArgumentException("相对原回答的分差不一致");
        }
        Integer expectedPreviousDelta = previousOverallScore == null
                ? null : evaluation.overallScore() - previousOverallScore;
        if (!Objects.equals(scoreDeltaFromPrevious, expectedPreviousDelta)) {
            throw new IllegalArgumentException("相对上次回答的分差不一致");
        }
        createdAt = Objects.requireNonNull(createdAt, "复习回答创建时间不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
