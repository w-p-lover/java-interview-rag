package com.javainterview.rag.interview.evaluation;

import java.time.LocalDateTime;
import java.util.Objects;

public record InterviewAnswerEvaluationLog(
        Long id,
        Mode mode,
        String baselineRunId,
        String baselineCaseId,
        String promptVersion,
        String rubricVersion,
        String modelName,
        Status status,
        Boolean knowledgeGrounded,
        Integer technicalScore,
        Integer completenessScore,
        Integer communicationScore,
        Integer overallScore,
        long elapsedMillis,
        String errorType,
        LocalDateTime createdAt
) {

    public InterviewAnswerEvaluationLog {
        Objects.requireNonNull(mode, "评价模式不能为空");
        baselineRunId = optionalText(baselineRunId);
        baselineCaseId = optionalText(baselineCaseId);
        if ((baselineRunId == null) != (baselineCaseId == null)) {
            throw new IllegalArgumentException("评分基线运行标识和样本标识必须同时存在");
        }
        if (mode != Mode.BASELINE && baselineRunId != null) {
            throw new IllegalArgumentException("正式或复习评价不能记录评分基线标识");
        }
        promptVersion = requireText(promptVersion, "提示词版本不能为空");
        rubricVersion = requireText(rubricVersion, "评分规则版本不能为空");
        modelName = requireText(modelName, "模型名称不能为空");
        Objects.requireNonNull(status, "评价状态不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("评价耗时不能为负数");
        }
        if (status == Status.SUCCEEDED) {
            Objects.requireNonNull(knowledgeGrounded, "成功评价必须记录知识依据状态");
            Objects.requireNonNull(technicalScore, "成功评价必须记录技术分");
            Objects.requireNonNull(completenessScore, "成功评价必须记录完整性分");
            Objects.requireNonNull(communicationScore, "成功评价必须记录表达分");
            Objects.requireNonNull(overallScore, "成功评价必须记录总分");
            if (errorType != null) {
                throw new IllegalArgumentException("成功评价不能记录失败类型");
            }
        } else if (errorType == null || errorType.isBlank()) {
            throw new IllegalArgumentException("失败评价必须记录失败类型");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public enum Mode {
        INTERVIEW,
        REVIEW,
        BASELINE
    }

    public enum Status {
        SUCCEEDED,
        FAILED
    }
}
