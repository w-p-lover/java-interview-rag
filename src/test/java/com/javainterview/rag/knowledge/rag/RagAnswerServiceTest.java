package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagAnswerServiceTest {

    @Test
    void answersFromReliableSourcesAndKeepsTheActualSources() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 在 Java 8 中使用数组、链表和红黑树。",
                "java/collections.md", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91);
        MarkdownSearchResult secondSource = new MarkdownSearchResult(
                "HashMap 的平均查询复杂度是 O(1)。",
                "java/performance.md", "version-2", 0,
                List.of("Java", "集合", "HashMap 性能"), 0.85);
        when(searchService.search("HashMap 底层结构", 5, 0.60)).thenReturn(List.of(source, secondSource));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("HashMap 使用数组、链表和红黑树 [资料 1]。");
        clearInvocations(chatClient);
        RagAnswerService service = new RagAnswerService(searchService, chatClient);

        RagAnswerResult result = service.answer("HashMap 底层结构");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(promptCaptor.capture());
        assertEquals("HashMap 使用数组、链表和红黑树 [资料 1]。", result.answer());
        assertEquals(true, result.grounded());
        assertEquals(List.of(source, secondSource), result.sources());
        assertEquals(true, promptCaptor.getValue().contains("问题：HashMap 底层结构"));
        assertEquals(true, promptCaptor.getValue().contains("[资料 1]"));
        assertEquals(true, promptCaptor.getValue().contains("[资料 2]"));
        assertEquals(true, promptCaptor.getValue().contains("来源：java/collections.md"));
    }

    @Test
    void returnsInsufficientAnswerWithoutCallingTheModelWhenNoReliableSourceExists() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(searchService.search("Java 虚拟线程", 5, 0.60)).thenReturn(List.of());
        RagAnswerService service = new RagAnswerService(searchService, chatClient);

        RagAnswerResult result = service.answer("Java 虚拟线程");

        assertEquals("知识库中没有足够可靠的资料回答这个问题。", result.answer());
        assertEquals(false, result.grounded());
        assertEquals(List.of(), result.sources());
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    void returnsInsufficientAnswerWhenTheModelReturnsBlankContent() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "ConcurrentHashMap 不允许 null 键或 null 值。",
                "java/concurrent.md", "version-2", 1,
                List.of("Java", "并发", "ConcurrentHashMap"), 0.88);
        when(searchService.search("ConcurrentHashMap 能否存 null", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("  ");
        RagAnswerService service = new RagAnswerService(searchService, chatClient);

        RagAnswerResult result = service.answer("ConcurrentHashMap 能否存 null");

        assertEquals("知识库中没有足够可靠的资料回答这个问题。", result.answer());
        assertEquals(false, result.grounded());
        assertEquals(List.of(source), result.sources());
    }
}
