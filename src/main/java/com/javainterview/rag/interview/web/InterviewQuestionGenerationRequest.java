package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "面试题生成请求")
public record InterviewQuestionGenerationRequest(
        @Schema(description = "目标岗位", example = "Java 后端工程师")
        String targetRole,
        @Schema(description = "面试准备资料")
        InterviewPreparationProfile profile,
        @Schema(description = "生成题目数量", example = "8")
        Integer questionCount,
        @Schema(description = "整场题数上限", example = "10")
        Integer maxQuestionCount,
        @Schema(description = "面试 Skill", example = "JAVA_CORE")
        InterviewSkill skill,
        @Schema(description = "面试难度", example = "INTERMEDIATE")
        InterviewDifficulty difficulty,
        @Schema(description = "准备画像 Prompt 版本", example = "interview-preparation-v1")
        String preparationPromptVersion
) {
}
