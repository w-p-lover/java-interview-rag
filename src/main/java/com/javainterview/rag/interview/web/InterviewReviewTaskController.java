package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewAttempt;
import com.javainterview.rag.interview.review.InterviewReviewAttemptConflictException;
import com.javainterview.rag.interview.review.InterviewReviewSessionHistory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/sessions/{sessionId}/review-tasks")
public class InterviewReviewTaskController {

    private final InterviewReviewTaskService service;

    public InterviewReviewTaskController(InterviewReviewTaskService service) {
        this.service = service;
    }

    @PostMapping
    public List<InterviewReviewTask> generate(@PathVariable String sessionId) {
        return service.generate(sessionId);
    }

    @GetMapping
    public List<InterviewReviewTask> findBySessionId(@PathVariable String sessionId) {
        return service.findBySessionId(sessionId);
    }

    @GetMapping("/history")
    public InterviewReviewSessionHistory findAttemptHistory(@PathVariable String sessionId) {
        return service.findAttemptHistory(sessionId);
    }

    @PatchMapping("/{taskId}/complete")
    public InterviewReviewTask complete(@PathVariable String sessionId,
                                        @PathVariable String taskId) {
        return service.complete(sessionId, taskId);
    }

    @GetMapping("/{taskId}/attempts")
    public List<InterviewReviewAttempt> findAttempts(@PathVariable String sessionId,
                                                     @PathVariable String taskId) {
        return service.findAttempts(sessionId, taskId);
    }

    @PostMapping("/{taskId}/attempts")
    public InterviewReviewAttempt submitAttempt(@PathVariable String sessionId,
                                                 @PathVariable String taskId,
                                                 @org.springframework.web.bind.annotation.RequestBody
                                                 InterviewReviewAttemptRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("复习回答请求不能为空");
        }
        return service.submitAttempt(sessionId, taskId, request.answer(),
                request.expectedAttemptNumber());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingSession(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(InterviewReviewAttemptConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAttemptConflict(InterviewReviewAttemptConflictException exception) {
        return Map.of("message", exception.getMessage());
    }
}
