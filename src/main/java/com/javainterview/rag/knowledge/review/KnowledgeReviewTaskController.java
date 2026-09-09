package com.javainterview.rag.knowledge.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/review-tasks")
@Tag(name = "知识复习任务", description = "将知识问答证据问题转为用户确认的复习任务")
public class KnowledgeReviewTaskController {
    private final KnowledgeReviewTaskService service;

    public KnowledgeReviewTaskController(KnowledgeReviewTaskService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "确认加入知识复习")
    public KnowledgeReviewTask create(@RequestBody KnowledgeReviewTaskRequest request) {
        if (request == null) throw new IllegalArgumentException("知识复习任务请求不能为空");
        return service.create(request.question(), request.claim(), request.evidenceStatus(), request.sourceNumbers());
    }

    @GetMapping
    @Operation(summary = "查询知识复习任务")
    public List<KnowledgeReviewTask> find(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedDateFrom,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedDateTo) {
        return service.find(status, plannedDateFrom, plannedDateTo);
    }

    @PatchMapping("/{taskId}/complete")
    public KnowledgeReviewTask complete(@PathVariable String taskId) { return service.complete(taskId); }

    @PostMapping("/{taskId}/attempts")
    public KnowledgeReviewAttempt submitAttempt(@PathVariable String taskId, @RequestBody KnowledgeReviewAttemptRequest request) {
        if (request == null) throw new IllegalArgumentException("复习回答请求不能为空");
        return service.submitAttempt(taskId, request.answer());
    }

    @GetMapping("/{taskId}/attempts")
    public List<KnowledgeReviewAttempt> findAttempts(@PathVariable String taskId) { return service.findAttempts(taskId); }

    @PatchMapping("/{taskId}/schedule")
    public KnowledgeReviewTask schedule(@PathVariable String taskId, @RequestBody KnowledgeReviewScheduleRequest request) {
        if (request == null) throw new IllegalArgumentException("复习计划请求不能为空");
        return service.schedule(taskId, request.plannedReviewDate());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) { return Map.of("message", exception.getMessage()); }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) { return Map.of("message", exception.getMessage()); }

    public record KnowledgeReviewScheduleRequest(LocalDate plannedReviewDate) { }
}
