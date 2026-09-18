package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.interview.question.InterviewQuestionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/questions")
@Tag(name = "面试题", description = "按岗位和准备资料生成面试题")
public class InterviewQuestionController {

    private final InterviewQuestionGenerationService questionGenerationService;
    private final InterviewQuestionPlanService questionPlanService;

    public InterviewQuestionController(InterviewQuestionGenerationService questionGenerationService,
                                       InterviewQuestionPlanService questionPlanService) {
        this.questionGenerationService = questionGenerationService;
        this.questionPlanService = questionPlanService;
    }

    @PostMapping
    @Operation(summary = "生成面试题")
    public InterviewQuestionGenerationResult generate(@RequestBody InterviewQuestionGenerationRequest request) {
        if (request.skill() != null || request.difficulty() != null
                || request.maxQuestionCount() != null || request.preparationPromptVersion() != null) {
            int questionCount = request.questionCount() == null ? 5 : request.questionCount();
            int maxQuestionCount = request.maxQuestionCount() == null
                    ? Math.max(questionCount, 8) : request.maxQuestionCount();
            return questionPlanService.generate(
                    request.targetRole(),
                    request.profile(),
                    questionCount,
                    maxQuestionCount,
                    request.skill(),
                    request.difficulty(),
                    request.preparationPromptVersion());
        }
        if (request.questionCount() == null) {
            return questionGenerationService.generate(request.targetRole(), request.profile());
        }
        return questionGenerationService.generate(request.targetRole(), request.profile(), request.questionCount());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
