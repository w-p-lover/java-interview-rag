package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "项目深挖题生成请求")
public record ProjectDeepDiveRequest(
        @Schema(description = "已由简历证据审计确认的项目事实", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfirmedProject project,
        @Schema(description = "是否只读检索知识库补充技术背景")
        boolean includeKnowledge
) {

    public record ConfirmedProject(
            ResumeEvidenceAuditResult.FactType type,
            String statement,
            String evidence
    ) {
    }
}
