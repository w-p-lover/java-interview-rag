package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "面试题生成请求")
public record InterviewQuestionGenerationRequest(
        @Schema(description = "目标岗位", example = "Java 后端工程师")
        String targetRole,
        @Schema(description = "面试准备资料")
        InterviewPreparationProfile profile,
        @Schema(description = "生成题目数量", example = "8")
        Integer questionCount
) {
}
