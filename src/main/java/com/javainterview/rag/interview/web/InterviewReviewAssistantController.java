package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogQueryService;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "复习助手", description = "基于复习任务提供查询和进展分析")
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
    @Operation(summary = "调用复习助手")
    public InterviewReviewAssistantResponse respond(
            @RequestBody InterviewReviewAssistantRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("复习助手请求不能为空");
        }
        InterviewReviewAssistantResult result = service.respond(
                request.sessionId(), request.message(), request.scheduleConfirmation());
        return new InterviewReviewAssistantResponse(result.requestId(), result.answer());
    }

    @GetMapping("/requests")
    @Operation(summary = "查询复习助手请求")
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
