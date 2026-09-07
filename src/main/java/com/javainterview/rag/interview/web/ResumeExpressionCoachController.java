package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeExpressionCoachRequest;
import com.javainterview.rag.interview.resume.ResumeExpressionCoachResult;
import com.javainterview.rag.interview.resume.ResumeExpressionCoachService;
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
@RequestMapping("/api/interviews/resume-expression")
@Tag(name = "简历表达教练", description = "基于已确认事实生成不覆盖原文的表达候选")
public class ResumeExpressionCoachController {

    private final ResumeExpressionCoachService coachService;

    public ResumeExpressionCoachController(ResumeExpressionCoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping
    @Operation(summary = "生成简历表达候选", description = "只使用已确认事实，结果不保存也不覆盖原始简历")
    public ResumeExpressionCoachResult coach(@RequestBody ResumeExpressionCoachRequest request) {
        return coachService.coach(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleCoachFailure(IllegalStateException exception) {
        return Map.of("message", exception.getMessage());
    }
}
