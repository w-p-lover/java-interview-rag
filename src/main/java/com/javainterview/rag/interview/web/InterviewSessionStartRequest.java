package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "开始模拟面试请求")
public record InterviewSessionStartRequest(
        @Schema(description = "目标岗位", example = "Java 后端工程师")
        String targetRole,
        @Schema(description = "简历原文")
        String resumeText,
        @Schema(description = "岗位描述")
        String jobDescription,
        @Schema(description = "首轮题目数量", example = "5")
        Integer initialQuestionCount,
        @Schema(description = "面试最大题目数量", example = "8")
        Integer maxQuestionCount
) {
}
