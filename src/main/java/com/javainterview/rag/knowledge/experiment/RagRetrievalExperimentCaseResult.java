package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult;

import java.util.Objects;

public record RagRetrievalExperimentCaseResult(
        String caseId,
        Status status,
        RagRetrievalEvaluationCaseResult result,
        String errorType
) {
    public RagRetrievalExperimentCaseResult {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("评估样本 ID 不能为空");
        }
        Objects.requireNonNull(status, "评估样本状态不能为空");
        if (status == Status.SUCCEEDED && result == null) {
            throw new IllegalArgumentException("成功样本必须包含评估结果");
        }
    }

    public static RagRetrievalExperimentCaseResult succeeded(
            RagRetrievalEvaluationCaseResult result) {
        return new RagRetrievalExperimentCaseResult(
                result.evaluationCaseId(), Status.SUCCEEDED, result, null);
    }

    public static RagRetrievalExperimentCaseResult failed(String caseId, Throwable failure) {
        return new RagRetrievalExperimentCaseResult(
                caseId, Status.FAILED, null, failure.getClass().getSimpleName());
    }

    public enum Status {
        SUCCEEDED,
        FAILED
    }
}
