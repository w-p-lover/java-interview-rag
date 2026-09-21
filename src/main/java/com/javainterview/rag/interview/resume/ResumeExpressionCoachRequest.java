package com.javainterview.rag.interview.resume;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "简历表达教练请求")
public record ResumeExpressionCoachRequest(
        @Schema(description = "目标岗位")
        String targetRole,
        @Schema(description = "已由简历证据审计确认的事实", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ResumeEvidenceAuditResult.ConfirmedFact> confirmedFacts,
        @Schema(description = "需要关联的不可变简历版本 ID")
        String resumeVersionId
) {

    public ResumeExpressionCoachRequest(
            String targetRole,
            List<ResumeEvidenceAuditResult.ConfirmedFact> confirmedFacts) {
        this(targetRole, confirmedFacts, null);
    }
}
