package com.javainterview.rag.interview.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class InterviewReviewAssistantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewReviewAssistantService.class);
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final String SYSTEM_PROMPT = """
            你是 Java 面试复习助手，只能帮助用户查询单个会话内的复习任务、分析本场练习进步，或为指定任务设置计划复习日期。
            查询任务清单时必须使用 list_review_tasks；分析练习次数、分数变化或未解决遗漏时必须使用 get_review_progress；设置日期时必须使用 schedule_review_task。
            复盘进展必须基于工具返回的持久化数据，未练习题目不得描述为退步，分数为空时不得猜测。
            当前会话由服务端结构化绑定；只有用户明确提供任务 ID 和 yyyy-MM-dd 日期并明确要求设置时，才能调用写入工具。
            不得猜测、补全或替换任务 ID 和日期；信息不足时直接说明缺少什么，不调用写入工具。
            工具成功后只简洁说明真实结果；工具失败时如实说明失败，不得声称已经完成操作。
            不得对用户输入是否乱码、编码异常或表达有误作无依据判断。
            """;
    private static final String SESSION_SCOPE_PROMPT = """
            当前请求只允许访问面试会话 %s。调用任何工具时必须原样使用这个会话 ID，不得根据用户消息切换会话。
            """;
    private static final String UNCONFIRMED_PROMPT = """
            本次请求未提供结构化人工确认，不得调用 schedule_review_task。
            如果用户希望改期，请复述会话 ID、任务 ID 和日期，并要求用户确认后重新提交。
            """;
    private static final String CONFIRMED_PROMPT = """
            本次请求已提供结构化人工确认。只有用户消息明确要求执行同一项改期时才可调用 schedule_review_task；
            服务端会拒绝与确认内容不一致的任何工具参数。
            """;

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final InterviewAssistantRequestLogStore requestLogStore;
    private final String modelName;

    public InterviewReviewAssistantService(ChatClient chatClient,
                                           ToolCallbackProvider toolCallbackProvider,
                                           InterviewAssistantRequestLogStore requestLogStore,
                                           String modelName) {
        this.chatClient = Objects.requireNonNull(chatClient, "ChatClient 不能为空");
        this.toolCallbackProvider = Objects.requireNonNull(
                toolCallbackProvider, "复习工具 Provider 不能为空");
        this.requestLogStore = Objects.requireNonNull(
                requestLogStore, "复习助手请求日志存储不能为空");
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("复习助手模型名称不能为空");
        }
        this.modelName = modelName.strip();
    }

    public InterviewReviewAssistantResult respond(
            String sessionId,
            String message,
            InterviewReviewScheduleConfirmation scheduleConfirmation) {
        String normalizedSessionId = requireText(sessionId, "复习助手会话 ID 不能为空");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("复习助手消息不能为空");
        }
        String normalizedMessage = message.strip();
        if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("复习助手消息不能超过 2000 个字符");
        }
        if (scheduleConfirmation != null
                && !normalizedSessionId.equals(scheduleConfirmation.sessionId())) {
            throw new IllegalArgumentException("改期确认会话与当前会话不一致");
        }
        InterviewAssistantRequestContext requestContext =
                InterviewAssistantRequestContext.create(
                        normalizedSessionId, scheduleConfirmation);
        long startedAt = System.nanoTime();
        ChatResponse response = null;
        try {
            response = chatClient.prompt()
                    .system(systemPrompt(normalizedSessionId, scheduleConfirmation))
                    .user(normalizedMessage)
                    .toolContext(requestContext.asMap())
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .chatResponse();
            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("复习助手未返回有效内容");
            }
            recordRequest(requestContext, InterviewAssistantRequestLog.Status.SUCCEEDED,
                    response, true, null, startedAt);
            return new InterviewReviewAssistantResult(requestContext.requestId(), content);
        } catch (RuntimeException exception) {
            recordRequest(requestContext, InterviewAssistantRequestLog.Status.FAILED,
                    response, false, exception.getClass().getSimpleName(), startedAt);
            throw exception;
        }
    }

    private String systemPrompt(String sessionId,
                                InterviewReviewScheduleConfirmation scheduleConfirmation) {
        return SYSTEM_PROMPT + SESSION_SCOPE_PROMPT.formatted(sessionId)
                + (scheduleConfirmation == null
                ? UNCONFIRMED_PROMPT : CONFIRMED_PROMPT);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private void recordRequest(InterviewAssistantRequestContext requestContext,
                               InterviewAssistantRequestLog.Status status,
                               ChatResponse response,
                               boolean finalResponsePresent,
                               String errorType,
                               long startedAt) {
        TokenUsage tokenUsage = tokenUsage(response);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        try {
            requestLogStore.record(new InterviewAssistantRequestLog(
                    null,
                    requestContext.requestId(),
                    modelName,
                    status,
                    requestContext.toolExecutionCount(),
                    tokenUsage.promptTokens(),
                    tokenUsage.completionTokens(),
                    tokenUsage.totalTokens(),
                    elapsedMillis,
                    finalResponsePresent,
                    errorType,
                    LocalDateTime.now()));
        } catch (RuntimeException exception) {
            LOGGER.warn("记录复习助手请求日志失败，requestId={}",
                    requestContext.requestId(), exception);
        }
    }

    private TokenUsage tokenUsage(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return TokenUsage.empty();
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null || usage.getPromptTokens() == null
                || usage.getCompletionTokens() == null || usage.getTotalTokens() == null) {
            return TokenUsage.empty();
        }
        return new TokenUsage(
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private record TokenUsage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {

        static TokenUsage empty() {
            return new TokenUsage(null, null, null);
        }
    }
}
