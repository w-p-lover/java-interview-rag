package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "简历证据审计请求")
public record ResumeEvidenceAuditRequest(
        @Schema(description = "待审计的简历原文", requiredMode = Schema.RequiredMode.REQUIRED)
        String resumeText) {
}
