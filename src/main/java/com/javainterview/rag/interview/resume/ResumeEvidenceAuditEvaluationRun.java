package com.javainterview.rag.interview.resume;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ResumeEvidenceAuditEvaluationRun(
        String runId,
        String datasetVersion,
        String role,
        String promptVersion,
        String modelName,
        Status status,
        int totalCaseCount,
        List<FixedResumeEvidenceAuditEvaluationService.CaseResult> caseResults,
        String errorType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {

    public ResumeEvidenceAuditEvaluationRun {
        runId = requireText(runId, "简历审计评估运行标识不能为空");
        datasetVersion = requireText(datasetVersion, "简历审计评估数据集版本不能为空");
        role = requireText(role, "简历审计评估角色不能为空");
        promptVersion = requireText(promptVersion, "简历审计评估 Prompt 版本不能为空");
        modelName = requireText(modelName, "简历审计评估模型名称不能为空");
        status = Objects.requireNonNull(status, "简历审计评估运行状态不能为空");
        if (totalCaseCount <= 0) {
            throw new IllegalArgumentException("简历审计评估样本数必须大于 0");
        }
        caseResults = List.copyOf(Objects.requireNonNull(caseResults, "简历审计评估样本结果不能为空"));
        createdAt = Objects.requireNonNull(createdAt, "简历审计评估创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "简历审计评估更新时间不能为空");
    }

    public boolean terminal() {
        return status == Status.SUCCEEDED || status == Status.PARTIAL_FAILED;
    }

    public enum Status {
        QUEUED,
        RUNNING,
        SUCCEEDED,
        PARTIAL_FAILED
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
