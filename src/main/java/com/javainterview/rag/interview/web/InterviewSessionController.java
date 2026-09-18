package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveSessionStartService;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewConversationContext;
import com.javainterview.rag.interview.session.InterviewConversationEvent;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.List;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/sessions")
@Tag(name = "模拟面试", description = "面试会话创建、查询与回答提交")
public class InterviewSessionController {

    private final InterviewSessionService sessionService;
    private final InterviewSessionStartService startService;
    private final ProjectDeepDiveSessionStartService projectDeepDiveStartService;

    public InterviewSessionController(InterviewSessionService sessionService,
                                      InterviewSessionStartService startService,
                                      ProjectDeepDiveSessionStartService projectDeepDiveStartService) {
        this.sessionService = sessionService;
        this.startService = startService;
        this.projectDeepDiveStartService = projectDeepDiveStartService;
    }

    @PostMapping("/start-project-deep-dive")
    @Operation(summary = "开始项目深挖面试", description = "围绕已确认项目事实创建固定四问会话")
    public InterviewSession startProjectDeepDive(
            @RequestBody ProjectDeepDiveSessionStartRequest request) {
        return projectDeepDiveStartService.start(request.targetRole(), request.toDeepDiveRequest());
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
        if (request.configuration() == null) {
            return sessionService.start(
                    request.targetRole(), request.profile(), request.questions(), maxQuestionCount);
        }
        return sessionService.start(
                request.targetRole(), request.profile(), request.questions(), maxQuestionCount,
                request.configuration(), request.requestId());
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

    @PostMapping(value = "/{sessionId}/answers/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提交面试回答", description = "先推送评价阶段事件，最后推送完整会话结果")
    public SseEmitter submitAnswerStream(@PathVariable String sessionId,
                                         @RequestBody InterviewAnswerSubmissionRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                InterviewSession existing = sessionService.find(sessionId);
                send(emitter, event("ANSWER_SUBMITTED", 1, request.questionIndex(),
                        "已收到回答，开始处理"));
                send(emitter, event("EVALUATION_STARTED", 2, request.questionIndex(),
                        "正在结合本场上下文进行结构化评价"));
                InterviewSession updated = sessionService.submitAnswer(
                        sessionId, request.questionIndex(), request.expectedVersion(), request.answer(),
                        InterviewConversationContext.from(existing));
                send(emitter, event("EVALUATION_COMPLETED", 3, request.questionIndex(),
                        "结构化评价已完成"));
                if (updated.status() == com.javainterview.rag.interview.session.InterviewSessionStatus.COMPLETED) {
                    send(emitter, event("SESSION_COMPLETED", 4, request.questionIndex(), "本场面试已完成"));
                } else if (updated.questions().size() > existing.questions().size()) {
                    send(emitter, event("FOLLOW_UP_GENERATED", 4, updated.currentQuestionIndex(),
                            "已生成下一步追问"));
                }
                emitter.send(SseEmitter.event().name("session").data(updated));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", exception.getMessage())));
                } catch (Exception ignored) {
                    // 连接已关闭时无需重复处理
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/{sessionId}/events")
    @Operation(summary = "查询会话事件时间线", description = "根据已保存会话状态重建阶段事件，不保存回答正文")
    public List<InterviewConversationEvent> events(@PathVariable String sessionId) {
        InterviewSession session = sessionService.find(sessionId);
        List<InterviewConversationEvent> events = new java.util.ArrayList<>();
        int sequence = 1;
        for (var answer : session.answers()) {
            events.add(event("ANSWER_SUBMITTED", sequence++, answer.questionIndex(), "已提交回答"));
            events.add(event("EVALUATION_STARTED", sequence++, answer.questionIndex(), "开始结构化评价"));
            events.add(event("EVALUATION_COMPLETED", sequence++, answer.questionIndex(), "结构化评价已完成"));
        }
        if (session.status() == com.javainterview.rag.interview.session.InterviewSessionStatus.COMPLETED) {
            events.add(event("SESSION_COMPLETED", sequence, session.currentQuestionIndex() - 1, "本场面试已完成"));
        } else if (session.currentQuestionIndex() > 0) {
            events.add(event("FOLLOW_UP_GENERATED", sequence, session.currentQuestionIndex(), "当前题目已准备就绪"));
        }
        return events;
    }

    private void send(SseEmitter emitter, InterviewConversationEvent event) throws java.io.IOException {
        emitter.send(SseEmitter.event().name("stage").data(event));
    }

    private InterviewConversationEvent event(String type, int sequence, int questionIndex, String summary) {
        return new InterviewConversationEvent(type, sequence, questionIndex, summary,
                java.time.LocalDateTime.now());
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
