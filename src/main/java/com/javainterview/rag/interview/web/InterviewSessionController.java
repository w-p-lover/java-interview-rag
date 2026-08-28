package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "模拟面试", description = "面试会话创建、查询与回答提交")
public class InterviewSessionController {

    private final InterviewSessionService sessionService;
    private final InterviewSessionStartService startService;

    public InterviewSessionController(InterviewSessionService sessionService,
                                      InterviewSessionStartService startService) {
        this.sessionService = sessionService;
        this.startService = startService;
    }

    @PostMapping("/start")
    @Operation(summary = "开始一次模拟面试", description = "根据岗位、简历和岗位描述生成面试会话")
    public InterviewSession start(@RequestBody InterviewSessionStartRequest request) {
        return startService.start(
                request.targetRole(),
                request.resumeText(),
                request.jobDescription(),
                request.initialQuestionCount(),
                request.maxQuestionCount());
    }

    @PostMapping
    @Operation(summary = "创建面试会话")
    public InterviewSession create(@RequestBody InterviewSessionCreateRequest request) {
        if (request.questions() == null) {
            throw new IllegalArgumentException("面试题不能为空");
        }
        int maxQuestionCount = request.maxQuestionCount() == null
                ? Math.max(request.questions().size(), 8) : request.maxQuestionCount();
        return sessionService.start(
                request.targetRole(), request.profile(), request.questions(), maxQuestionCount);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "查询面试会话")
    public InterviewSession find(@PathVariable String sessionId) {
        return sessionService.find(sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    @Operation(summary = "提交面试回答", description = "按题目序号提交回答并进行版本校验")
    public InterviewSession submitAnswer(@PathVariable String sessionId,
                                         @RequestBody InterviewAnswerSubmissionRequest request) {
        return sessionService.submitAnswer(
                sessionId, request.questionIndex(), request.expectedVersion(), request.answer());
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
