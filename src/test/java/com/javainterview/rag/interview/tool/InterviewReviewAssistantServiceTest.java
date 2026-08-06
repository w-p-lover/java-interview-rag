package com.javainterview.rag.interview.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewReviewAssistantServiceTest {

    @Test
    void bindsOnlyConfiguredReviewToolsToChatRequest() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.user("查询会话 session-1 的待复习任务")).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(toolCallbackProvider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.chatResponse()).thenReturn(chatResponse(
                "已查询到 1 条待复习任务。", 120, 30, 150));
        InterviewAssistantRequestLogStore requestLogStore =
                mock(InterviewAssistantRequestLogStore.class);
        InterviewReviewAssistantService service =
                new InterviewReviewAssistantService(
                        chatClient, toolCallbackProvider, requestLogStore, "qwen3.7-plus");

        InterviewReviewAssistantResult result =
                service.respond(" 查询会话 session-1 的待复习任务 ", null);

        assertEquals("已查询到 1 条待复习任务。", result.answer());
        assertTrue(result.requestId().matches("[0-9a-f-]{36}"));
        verify(requestSpec).system(argThat((String prompt) ->
                prompt.contains("不得猜测")
                        && prompt.contains("明确要求")
                        && prompt.contains("get_review_progress")
                        && prompt.contains("练习进步")
                        && prompt.contains("未提供结构化人工确认")
                        && prompt.contains("不得对用户输入是否乱码")));
        verify(requestSpec).toolContext(argThat((Map<String, Object> context) ->
                context.containsKey(InterviewAssistantRequestContext.KEY)));
        verify(requestSpec).toolCallbacks(toolCallbackProvider);
        verify(requestLogStore).record(argThat(log ->
                result.requestId().equals(log.requestId())
                        && "qwen3.7-plus".equals(log.modelName())
                        && log.status() == InterviewAssistantRequestLog.Status.SUCCEEDED
                        && log.toolExecutionCount() == 0
                        && log.promptTokens() == 120
                        && log.completionTokens() == 30
                        && log.totalTokens() == 150
                        && log.finalResponsePresent()
                        && log.errorType() == null));
    }

    @Test
    void rejectsBlankOrOversizedMessageBeforeCallingModel() {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewReviewAssistantService service = new InterviewReviewAssistantService(
                chatClient,
                mock(ToolCallbackProvider.class),
                mock(InterviewAssistantRequestLogStore.class),
                "qwen3.7-plus");

        IllegalArgumentException blankException = assertThrows(
                IllegalArgumentException.class, () -> service.respond(" "));
        IllegalArgumentException oversizedException = assertThrows(
                IllegalArgumentException.class, () -> service.respond("a".repeat(2001)));

        assertEquals("复习助手消息不能为空", blankException.getMessage());
        assertEquals("复习助手消息不能超过 2000 个字符", oversizedException.getMessage());
        verifyNoInteractions(chatClient);
    }

    @Test
    void passesExactScheduleConfirmationThroughHiddenToolContext() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.user("确认改期")).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(toolCallbackProvider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.chatResponse()).thenReturn(chatResponse(
                "已完成改期。", 100, 20, 120));
        InterviewReviewAssistantService service = new InterviewReviewAssistantService(
                chatClient,
                toolCallbackProvider,
                mock(InterviewAssistantRequestLogStore.class),
                "qwen3.7-plus");
        InterviewReviewScheduleConfirmation confirmation =
                new InterviewReviewScheduleConfirmation(
                        "session-1", "task-1", LocalDate.of(2026, 8, 12));

        service.respond("确认改期", confirmation);

        verify(requestSpec).system(argThat((String prompt) ->
                prompt.contains("已提供结构化人工确认")));
        verify(requestSpec).toolContext(argThat((Map<String, Object> context) -> {
            InterviewAssistantRequestContext requestContext =
                    (InterviewAssistantRequestContext) context.get(
                            InterviewAssistantRequestContext.KEY);
            requestContext.requireScheduleConfirmation(
                    "session-1", "task-1", LocalDate.of(2026, 8, 12));
            return true;
        }));
    }

    @Test
    void rejectsEmptyModelResponse() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.user("查询任务")).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(toolCallbackProvider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.chatResponse()).thenReturn(chatResponse(" ", null, null, null));
        InterviewAssistantRequestLogStore requestLogStore =
                mock(InterviewAssistantRequestLogStore.class);
        InterviewReviewAssistantService service =
                new InterviewReviewAssistantService(
                        chatClient, toolCallbackProvider, requestLogStore, "qwen3.7-plus");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.respond("查询任务"));

        assertEquals("复习助手未返回有效内容", exception.getMessage());
        verify(requestLogStore).record(argThat(log ->
                log.status() == InterviewAssistantRequestLog.Status.FAILED
                        && "IllegalStateException".equals(log.errorType())
                        && !log.finalResponsePresent()));
    }

    @Test
    void recordsFailureTypeAndRethrowsOriginalModelException() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        InterviewAssistantRequestLogStore requestLogStore =
                mock(InterviewAssistantRequestLogStore.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.user("查询任务")).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(toolCallbackProvider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new IllegalStateException("上游返回的敏感详情"));
        InterviewReviewAssistantService service = new InterviewReviewAssistantService(
                chatClient, toolCallbackProvider, requestLogStore, "qwen3.7-plus");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.respond("查询任务"));

        assertEquals("上游返回的敏感详情", exception.getMessage());
        verify(requestLogStore).record(argThat(log ->
                log.status() == InterviewAssistantRequestLog.Status.FAILED
                        && "IllegalStateException".equals(log.errorType())
                        && log.promptTokens() == null
                        && log.errorType().equals(exception.getClass().getSimpleName())));
    }

    @Test
    void doesNotFailAssistantResponseWhenRequestLogCannotBeStored() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        InterviewAssistantRequestLogStore requestLogStore =
                mock(InterviewAssistantRequestLogStore.class);
        when(chatClient.prompt().system(org.mockito.ArgumentMatchers.anyString())
                .user("查询任务").toolContext(anyMap())
                .toolCallbacks(toolCallbackProvider).call().chatResponse())
                .thenReturn(chatResponse("没有待复习任务。", 40, 10, 50));
        doThrow(new IllegalStateException("日志表不可用"))
                .when(requestLogStore).record(org.mockito.ArgumentMatchers.any());
        InterviewReviewAssistantService service = new InterviewReviewAssistantService(
                chatClient, toolCallbackProvider, requestLogStore, "qwen3.7-plus");

        InterviewReviewAssistantResult result = service.respond("查询任务");

        assertEquals("没有待复习任务。", result.answer());
    }

    private ChatResponse chatResponse(String content,
                                      Integer promptTokens,
                                      Integer completionTokens,
                                      Integer totalTokens) {
        ChatResponseMetadata metadata = promptTokens == null
                ? ChatResponseMetadata.builder().build()
                : ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(promptTokens, completionTokens, totalTokens))
                        .build();
        return new ChatResponse(
                List.of(new Generation(new AssistantMessage(content))),
                metadata);
    }
}
