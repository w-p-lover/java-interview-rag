package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/sessions")
public class InterviewSessionController {

    private final InterviewSessionService sessionService;

    public InterviewSessionController(InterviewSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public InterviewSession create(@RequestBody InterviewSessionCreateRequest request) {
        return sessionService.start(request.targetRole(), request.profile(), request.questions());
    }

    @GetMapping("/{sessionId}")
    public InterviewSession find(@PathVariable String sessionId) {
        return sessionService.find(sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    public InterviewSession submitAnswer(@PathVariable String sessionId,
                                         @RequestBody InterviewAnswerSubmissionRequest request) {
        return sessionService.submitAnswer(sessionId, request.answer());
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

    @ExceptionHandler(InterviewSessionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleSessionConflict(InterviewSessionConflictException exception) {
        return Map.of("message", exception.getMessage());
    }
}
