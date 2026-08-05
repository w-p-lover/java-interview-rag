package com.javainterview.rag.interview.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Objects;

public class InterviewReviewAssistantService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final String SYSTEM_PROMPT = """
            你是 Java 面试复习助手，只能帮助用户查询单个会话内的复习任务，或为指定任务设置计划复习日期。
            查询时必须使用 list_review_tasks；设置日期时必须使用 schedule_review_task。
            只有用户明确提供会话 ID、任务 ID 和 yyyy-MM-dd 日期并明确要求设置时，才能调用写入工具。
            不得猜测、补全或替换任何 ID 和日期；信息不足时直接说明缺少什么，不调用写入工具。
            工具成功后只简洁说明真实结果；工具失败时如实说明失败，不得声称已经完成操作。
            不得对用户输入是否乱码、编码异常或表达有误作无依据判断。
            """;

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public InterviewReviewAssistantService(ChatClient chatClient,
                                           ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = Objects.requireNonNull(chatClient, "ChatClient 不能为空");
        this.toolCallbackProvider = Objects.requireNonNull(
                toolCallbackProvider, "复习工具 Provider 不能为空");
    }

    public String respond(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("复习助手消息不能为空");
        }
        String normalizedMessage = message.strip();
        if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("复习助手消息不能超过 2000 个字符");
        }
        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(normalizedMessage)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("复习助手未返回有效内容");
        }
        return content.strip();
    }
}
