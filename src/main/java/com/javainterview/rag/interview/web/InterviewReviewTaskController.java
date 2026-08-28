package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.interview.review.InterviewReviewAttempt;
import com.javainterview.rag.interview.review.InterviewReviewAttemptConflictException;
import com.javainterview.rag.interview.review.InterviewReviewSessionHistory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/sessions/{sessionId}/review-tasks")
@Tag(name = "复习任务", description = "生成、查询和完成面试复习任务")
public class InterviewReviewTaskController {

    private final InterviewReviewTaskService service;

    public InterviewReviewTaskController(InterviewReviewTaskService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "生成复习任务")
    public List<InterviewReviewTask> generate(@PathVariable String sessionId) {
        return service.generate(sessionId);
    }

    @GetMapping
    @Operation(summary = "查询复习任务")
    public List<InterviewReviewTask> findBySessionId(
            @PathVariable String sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedDateTo) {
        return service.findBySessionId(sessionId, status, plannedDateFrom, plannedDateTo);
    }

    @GetMapping("/history")
    @Operation(summary = "查询复习历史")
    public InterviewReviewSessionHistory findAttemptHistory(@PathVariable String sessionId) {
        return service.findAttemptHistory(sessionId);
    }

    @PatchMapping("/{taskId}/complete")
    @Operation(summary = "完成复习任务")
    public InterviewReviewTask complete(@PathVariable String sessionId,
                                        @PathVariable String taskId) {
        return service.complete(sessionId, taskId);
    }

    @PatchMapping("/{taskId}/schedule")
    @Operation(summary = "安排复习日期")
    public InterviewReviewTask schedule(@PathVariable String sessionId,
                                        @PathVariable String taskId,
                                        @RequestBody InterviewReviewScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("复习计划请求不能为空");
        }
        return service.schedule(sessionId, taskId, request.plannedReviewDate());
    }

    @GetMapping("/{taskId}/attempts")
    @Operation(summary = "查询复习回答记录")
    public List<InterviewReviewAttempt> findAttempts(@PathVariable String sessionId,
                                                     @PathVariable String taskId) {
        return service.findAttempts(sessionId, taskId);
    }

    @PostMapping("/{taskId}/attempts")
    @Operation(summary = "提交复习回答")
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
