package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewPathResult;
import com.javainterview.rag.interview.review.InterviewReviewPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/review-path")
@Tag(name = "复习路径", description = "基于多个已知面试会话生成确定性复习推荐")
public class InterviewReviewPathController {

    private final InterviewReviewPathService service;

    public InterviewReviewPathController(InterviewReviewPathService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "生成跨会话复习路径", description = "按任务、attempt 和计划日期生成确定性推荐，不调用模型")
    public InterviewReviewPathResult recommend(
            @RequestParam(name = "sessionIds") List<String> sessionIds) {
        return service.recommend(sessionIds);
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
}
