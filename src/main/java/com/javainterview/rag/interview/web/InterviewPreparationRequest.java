package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "面试准备请求")
public record InterviewPreparationRequest(
        @Schema(description = "目标岗位", example = "Java 后端工程师")
        String targetRole,
        @Schema(description = "简历原文")
        String resumeText,
        @Schema(description = "岗位描述")
        String jobDescription
) {
}
