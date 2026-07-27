package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
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
public class InterviewQuestionController {

    private final InterviewQuestionGenerationService questionGenerationService;

    public InterviewQuestionController(InterviewQuestionGenerationService questionGenerationService) {
        this.questionGenerationService = questionGenerationService;
    }

    @PostMapping
    public InterviewQuestionGenerationResult generate(@RequestBody InterviewQuestionGenerationRequest request) {
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
