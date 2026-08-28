package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLogQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/sessions/{sessionId}/tool-executions")
@Tag(name = "工具执行日志", description = "查询面试助手工具调用元数据")
public class InterviewToolExecutionLogController {

    private final InterviewToolExecutionLogQueryService queryService;

    public InterviewToolExecutionLogController(InterviewToolExecutionLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "查询工具执行日志")
    public List<InterviewToolExecutionLog> findRecent(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.findRecent(sessionId, limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
