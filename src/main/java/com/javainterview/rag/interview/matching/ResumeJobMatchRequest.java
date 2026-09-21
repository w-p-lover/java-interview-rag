package com.javainterview.rag.interview.matching;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "简历与岗位匹配请求")
public record ResumeJobMatchRequest(
        @Schema(description = "目标岗位", requiredMode = Schema.RequiredMode.REQUIRED)
        String targetRole,
        @Schema(description = "岗位描述原文", requiredMode = Schema.RequiredMode.REQUIRED)
        String jobDescription,
        @Schema(description = "已由简历证据审计确认的事实", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ConfirmedFact> confirmedFacts,
        @Schema(description = "需要关联的不可变简历版本 ID")
        String resumeVersionId
) {

    public ResumeJobMatchRequest(
            String targetRole,
            String jobDescription,
            List<ConfirmedFact> confirmedFacts) {
        this(targetRole, jobDescription, confirmedFacts, null);
    }

    public record ConfirmedFact(
            ResumeEvidenceAuditResult.FactType type,
            String statement,
            String evidence
    ) {
    }
}
