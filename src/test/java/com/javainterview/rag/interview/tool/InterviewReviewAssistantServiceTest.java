package com.javainterview.rag.interview.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
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
        when(requestSpec.toolCallbacks(toolCallbackProvider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("已查询到 1 条待复习任务。");
        InterviewReviewAssistantService service =
                new InterviewReviewAssistantService(chatClient, toolCallbackProvider);

        String answer = service.respond(" 查询会话 session-1 的待复习任务 ");

        assertEquals("已查询到 1 条待复习任务。", answer);
        verify(requestSpec).system(argThat((String prompt) ->
                prompt.contains("不得猜测")
                        && prompt.contains("明确要求")
                        && prompt.contains("不得对用户输入是否乱码")));
        verify(requestSpec).toolCallbacks(toolCallbackProvider);
    }

    @Test
    void rejectsBlankOrOversizedMessageBeforeCallingModel() {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewReviewAssistantService service = new InterviewReviewAssistantService(
                chatClient, mock(ToolCallbackProvider.class));

        IllegalArgumentException blankException = assertThrows(
                IllegalArgumentException.class, () -> service.respond(" "));
        IllegalArgumentException oversizedException = assertThrows(
                IllegalArgumentException.class, () -> service.respond("a".repeat(2001)));

        assertEquals("复习助手消息不能为空", blankException.getMessage());
        assertEquals("复习助手消息不能超过 2000 个字符", oversizedException.getMessage());
        verifyNoInteractions(chatClient);
    }

    @Test
    void rejectsEmptyModelResponse() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        when(chatClient.prompt().system(org.mockito.ArgumentMatchers.anyString())
                .user("查询任务").toolCallbacks(toolCallbackProvider).call().content())
                .thenReturn(" ");
        InterviewReviewAssistantService service =
                new InterviewReviewAssistantService(chatClient, toolCallbackProvider);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.respond("查询任务"));

        assertEquals("复习助手未返回有效内容", exception.getMessage());
    }
}
