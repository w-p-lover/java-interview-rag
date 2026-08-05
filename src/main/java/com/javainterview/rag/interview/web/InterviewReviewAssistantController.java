package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogQueryService;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantService;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantResult;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/review-assistant")
public class InterviewReviewAssistantController {

    private final InterviewReviewAssistantService service;
    private final InterviewAssistantRequestLogQueryService requestLogQueryService;

    public InterviewReviewAssistantController(
            InterviewReviewAssistantService service,
            InterviewAssistantRequestLogQueryService requestLogQueryService) {
        this.service = service;
        this.requestLogQueryService = requestLogQueryService;
    }

    @PostMapping
    public InterviewReviewAssistantResponse respond(
            @RequestBody InterviewReviewAssistantRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("复习助手请求不能为空");
        }
        InterviewReviewAssistantResult result = service.respond(request.message());
        return new InterviewReviewAssistantResponse(result.requestId(), result.answer());
    }

    @GetMapping("/requests")
    public List<InterviewAssistantRequestLog> findRecentRequests(
            @RequestParam(defaultValue = "20") int limit) {
        return requestLogQueryService.findRecent(limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleAssistantFailure(IllegalStateException exception) {
        return Map.of("message", exception.getMessage());
    }
}
