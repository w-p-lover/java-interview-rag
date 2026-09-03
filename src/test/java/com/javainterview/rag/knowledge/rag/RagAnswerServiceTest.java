package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagAnswerServiceTest {

    @Test
    void rejectsBlankQuestionBeforeSearchingOrRecording() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        assertEquals("检索问题不能为空",
                assertThrows(IllegalArgumentException.class, () -> service.answer(" ")).getMessage());

        verifyNoInteractions(searchService, chatClient, logStore);
    }

    @Test
    void answersFromReliableSourcesAndKeepsTheActualSources() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 在 Java 8 中使用数组、链表和红黑树。",
                "java/collections.pdf", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91,
                3, KnowledgeBlockType.CODE);
        MarkdownSearchResult secondSource = new MarkdownSearchResult(
                "HashMap 的平均查询复杂度是 O(1)。",
                "java/performance.md", "version-2", 0,
                List.of("Java", "集合", "HashMap 性能"), 0.85);
        when(searchService.search("HashMap 底层结构", 5, 0.60)).thenReturn(List.of(source, secondSource));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("HashMap 使用数组、链表和红黑树 [资料 1]。");
        clearInvocations(chatClient);
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap 底层结构");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(promptCaptor.capture());
        assertEquals("HashMap 使用数组、链表和红黑树 [资料 1]。", result.answer());
        assertEquals(true, result.grounded());
        assertEquals(List.of(source, secondSource), result.sources());
        assertEquals(true, promptCaptor.getValue().contains("问题：HashMap 底层结构"));
        assertEquals(true, promptCaptor.getValue().contains("[资料 1]"));
        assertEquals(true, promptCaptor.getValue().contains("[资料 2]"));
        assertEquals(true, promptCaptor.getValue().contains("来源：java/collections.pdf"));
        assertEquals(true, promptCaptor.getValue().contains("页码：第 3 页"));
        assertEquals(true, promptCaptor.getValue().contains("内容类型：代码"));
        assertEquals(true, promptCaptor.getValue().contains("把问题拆成需要回答的事实要点"));
        assertEquals(true, promptCaptor.getValue().contains("每个事实陈述只表达一个原子结论"));
        assertEquals(true, promptCaptor.getValue().contains("不要输出未引用的开场白"));
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.ANSWERED, logCaptor.getValue().status());
        assertEquals("rag-answer-v4", logCaptor.getValue().promptVersion());
        assertEquals(2, logCaptor.getValue().sources().size());
        assertEquals("java/collections.pdf", logCaptor.getValue().sources().get(0).source());
        assertEquals(3, logCaptor.getValue().sources().get(0).pageNumber());
        assertEquals(KnowledgeBlockType.CODE, logCaptor.getValue().sources().get(0).blockType());
    }

    @Test
    void returnsInsufficientAnswerWithoutCallingTheModelWhenNoReliableSourceExists() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        when(searchService.search("Java 虚拟线程", 5, 0.60)).thenReturn(List.of());
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("Java 虚拟线程");

        assertEquals("知识库中没有足够可靠的资料回答这个问题。", result.answer());
        assertEquals(false, result.grounded());
        assertEquals(List.of(), result.sources());
        verify(chatClient, never()).prompt(anyString());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.INSUFFICIENT, logCaptor.getValue().status());
        assertEquals(List.of(), logCaptor.getValue().sources());
    }

    @Test
    void expandsContextOnlyAfterInitialSearchReturnsReliableSources() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        MarkdownAdjacentChunkExpander contextExpander = mock(MarkdownAdjacentChunkExpander.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult seed = new MarkdownSearchResult(
                "当前段", "java/collections.md", "version-1", 1,
                List.of("Java", "集合"), 0.91);
        MarkdownSearchResult neighbor = new MarkdownSearchResult(
                "相邻段", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.55);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(seed));
        when(contextExpander.expand(List.of(seed))).thenReturn(List.of(seed, neighbor));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("基于资料回答 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, contextExpander, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap");

        assertEquals(List.of(seed, neighbor), result.sources());
        verify(contextExpander).expand(List.of(seed));
    }

    @Test
    void returnsInsufficientAnswerWhenTheModelReturnsBlankContent() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "ConcurrentHashMap 不允许 null 键或 null 值。",
                "java/concurrent.md", "version-2", 1,
                List.of("Java", "并发", "ConcurrentHashMap"), 0.88);
        when(searchService.search("ConcurrentHashMap 能否存 null", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("  ");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("ConcurrentHashMap 能否存 null");

        assertEquals("知识库中没有足够可靠的资料回答这个问题。", result.answer());
        assertEquals(false, result.grounded());
        assertEquals(List.of(source), result.sources());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.INSUFFICIENT, logCaptor.getValue().status());
    }

    @Test
    void rejectsAnAnswerThatReferencesANonexistentSource() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 会树化，使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("HashMap 会树化 [资料 2]。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap");

        assertEquals("知识库中没有足够可靠的资料回答这个问题。", result.answer());
        assertEquals(false, result.grounded());
        assertEquals(List.of(source), result.sources());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.CITATION_INVALID, logCaptor.getValue().status());
    }

    @Test
    void rejectsAnAnswerWithAnUncitedClaim() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 会树化，使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 会树化 [资料 1]。并发写入也绝对安全。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap");

        assertEquals(true, result.grounded());
        assertEquals("HashMap 会树化 [资料 1]。", result.answer());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.PARTIAL_ANSWER, logCaptor.getValue().status());
    }

    @Test
    void marksTheStreamCompletionAsInsufficientWhenCitationValidationFails() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).stream().content())
                .thenReturn(Flux.just("HashMap 会树化 ", "[资料 2]。"));
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        List<RagAnswerStreamEvent> events = service.streamAnswer("HashMap").collectList().block();

        RagAnswerStreamEvent complete = events.get(events.size() - 1);
        assertEquals("complete", complete.type());
        assertEquals("知识库中没有足够可靠的资料回答这个问题。", complete.content());
        assertEquals(false, complete.grounded());
        assertEquals(List.of(source), complete.sources());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.CITATION_INVALID, logCaptor.getValue().status());
        assertEquals(0, events.stream().filter(event -> event.type().equals("delta")).count());
    }

    @Test
    void recordsEvidenceInsufficientWhenCitationDoesNotSupportTheClaim() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组和链表。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("Redis", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("Redis 支持持久化 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("Redis");

        assertEquals(false, result.grounded());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.EVIDENCE_INSUFFICIENT, logCaptor.getValue().status());
    }

    @Test
    void recordsEvidenceConflictWhenCitationHasOppositeNegation() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "普通 HashMap 不提供并发安全。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap 并发", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("普通 HashMap 提供并发安全 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap 并发");

        assertEquals(false, result.grounded());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.EVIDENCE_CONFLICTING, logCaptor.getValue().status());
    }

    @Test
    void recordsFailureAndRethrowsWhenTheModelCallFails() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合", "HashMap"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenThrow(new IllegalStateException("模型不可用"));
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        assertEquals("模型不可用",
                assertThrows(IllegalStateException.class, () -> service.answer("HashMap")).getMessage());

        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.FAILED, logCaptor.getValue().status());
        assertEquals("IllegalStateException", logCaptor.getValue().errorType());
        assertEquals(List.of(RagRetrievalSource.from(source)), logCaptor.getValue().sources());
    }

    @Test
    void usesHybridRetrieverWhenConfigured() {
        MarkdownSimilaritySearchService denseSearchService = mock(MarkdownSimilaritySearchService.class);
        MarkdownHybridSearchService hybridSearchService = mock(MarkdownHybridSearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "treeifyBin 在冲突链达到阈值后参与树化。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.02);
        when(hybridSearchService.search("treeifyBin", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("treeifyBin 在冲突链达到阈值后参与树化 [资料 1]。");
        clearInvocations(chatClient);
        RagAnswerService service = new RagAnswerService(denseSearchService, hybridSearchService,
                null, chatClient, logStore);

        RagAnswerResult result = service.answer("treeifyBin");

        assertEquals(true, result.grounded());
        assertEquals(List.of(source), result.sources());
        verify(hybridSearchService).search("treeifyBin", 5, 0.60);
        verifyNoInteractions(denseSearchService);
    }
}
