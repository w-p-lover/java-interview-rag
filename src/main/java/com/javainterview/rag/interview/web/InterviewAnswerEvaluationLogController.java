package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/logs")
public class InterviewAnswerEvaluationLogController {

    private final InterviewAnswerEvaluationLogQueryService queryService;

    public InterviewAnswerEvaluationLogController(InterviewAnswerEvaluationLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public InterviewAnswerEvaluationLogQueryService.Report findRecent(
            @RequestParam(defaultValue = "BASELINE") String mode,
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.findRecent(parseMode(mode), limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    private InterviewAnswerEvaluationLog.Mode parseMode(String mode) {
        try {
            return InterviewAnswerEvaluationLog.Mode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("评价模式必须是 INTERVIEW、REVIEW 或 BASELINE");
        }
    }
}
