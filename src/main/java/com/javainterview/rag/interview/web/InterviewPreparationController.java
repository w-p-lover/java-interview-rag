package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
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
@RequestMapping("/api/interviews/preparations")
@Tag(name = "面试准备", description = "生成面试准备方向与知识来源")
public class InterviewPreparationController {

    private final InterviewPreparationService preparationService;

    public InterviewPreparationController(InterviewPreparationService preparationService) {
        this.preparationService = preparationService;
    }

    @PostMapping
    @Operation(summary = "生成面试准备建议")
    public InterviewPreparationResult prepare(@RequestBody InterviewPreparationRequest request) {
        return preparationService.prepare(request.targetRole(), request.resumeText(), request.jobDescription());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
