package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.token.TokenBudget;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalUnavailableException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertEquals(true, promptCaptor.getValue().contains("不要输出问题拆解过程"));
        assertEquals(true, promptCaptor.getValue().contains("不要输出未引用的开场白"));
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.ANSWERED, logCaptor.getValue().status());
        assertEquals("rag-answer-v6", logCaptor.getValue().promptVersion());
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
        assertEquals(1, complete.claimEvidence().size());
        assertEquals(RagAnswerClaimEvidenceStatus.CITATION_INVALID,
                complete.claimEvidence().get(0).status());
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
        when(hybridSearchService.searchDetailed("treeifyBin", 5, 0.60)).thenReturn(
                new MarkdownHybridSearchService.MarkdownHybridSearchResult(
                        "treeifyBin", 5, 1, 1,
                        List.of(new MarkdownHybridSearchService.MarkdownHybridSearchHit(
                                source, 1, 1, 0.91, 2.0, 0.03)), 2));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("treeifyBin 在冲突链达到阈值后参与树化 [资料 1]。");
        clearInvocations(chatClient);
        RagAnswerService service = new RagAnswerService(denseSearchService, hybridSearchService,
                null, chatClient, logStore);

        RagAnswerResult result = service.answer("treeifyBin");

        assertEquals(true, result.grounded());
        assertEquals(List.of(source), result.sources());
        assertEquals("HYBRID", result.retrievalStrategy());
        verify(hybridSearchService).searchDetailed("treeifyBin", 5, 0.60);
        verifyNoInteractions(denseSearchService);
    }

    @Test
    void exposesSparseOnlyDegradationWithoutBypassingEvidenceValidation() {
        MarkdownSimilaritySearchService denseSearchService = mock(MarkdownSimilaritySearchService.class);
        MarkdownHybridSearchService hybridSearchService = mock(MarkdownHybridSearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 2.4);
        when(hybridSearchService.searchDetailed("HashMap", 5, 0.60)).thenReturn(
                new MarkdownHybridSearchService.MarkdownHybridSearchResult(
                        "HashMap", 5, 0, 1,
                        List.of(new MarkdownHybridSearchService.MarkdownHybridSearchHit(
                                source, null, 1, null, 2.4, 0.016)), 3,
                        MarkdownHybridSearchService.RetrievalStrategy.SPARSE_ONLY,
                        true, "IllegalStateException"));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 使用数组、链表和红黑树 [资料 1]。");
        RagAnswerService service = new RagAnswerService(denseSearchService, hybridSearchService,
                null, chatClient, logStore);

        RagAnswerResult result = service.answer("HashMap");

        assertEquals(true, result.grounded());
        assertEquals("SPARSE_ONLY", result.retrievalStrategy());
        assertEquals(true, result.degraded());
        assertEquals("IllegalStateException", result.degradationReason());
    }

    @Test
    void returnsRetrievalUnavailableWithoutCallingChatForSyncAndStream() {
        MarkdownSimilaritySearchService denseSearchService = mock(MarkdownSimilaritySearchService.class);
        MarkdownHybridSearchService hybridSearchService = mock(MarkdownHybridSearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        when(hybridSearchService.searchDetailed("HashMap", 5, 0.60))
                .thenThrow(new KnowledgeRetrievalUnavailableException("Dense 与 Sparse 检索均不可用"));
        RagAnswerService service = new RagAnswerService(denseSearchService, hybridSearchService,
                null, chatClient, logStore);

        RagAnswerResult sync = service.answer("HashMap");
        List<RagAnswerStreamEvent> streamEvents = service.streamAnswer("HashMap").collectList().block();
        RagAnswerStreamEvent streamed = streamEvents.get(streamEvents.size() - 1);

        assertEquals("知识检索暂时不可用，请稍后重试。", sync.answer());
        assertEquals(sync.answer(), streamed.content());
        assertEquals("UNAVAILABLE", sync.retrievalStrategy());
        assertEquals(true, sync.degraded());
        assertEquals(List.of(), sync.sources());
        assertEquals(List.of(), streamed.sources());
        verifyNoInteractions(chatClient, denseSearchService);
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore, org.mockito.Mockito.times(2)).record(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream().allMatch(log ->
                log.status() == RagRetrievalLogStatus.RETRIEVAL_UNAVAILABLE
                        && "KnowledgeRetrievalUnavailableException".equals(log.errorType())));
    }

    @Test
    void rewritesReferenceQuestionWithTheLatestHistoryTopicBeforeRetrieval() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组、链表和红黑树。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap 底层结构；追问：它为什么会树化", 5, 0.60))
                .thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 会树化 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        RagAnswerResult result = service.answer("它为什么会树化",
                List.of(new RagConversationTurn("HashMap 底层结构", "HashMap 使用数组、链表和红黑树。")));

        verify(searchService).search("HashMap 底层结构；追问：它为什么会树化", 5, 0.60);
    }

    @Test
    void boundsHistoryBeforeAddingItToThePrompt() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 使用数组 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);
        String longAnswer = "回答".repeat(2_000);

        service.answer("HashMap", List.of(
                new RagConversationTurn("旧问题 1", longAnswer),
                new RagConversationTurn("旧问题 2", longAnswer),
                new RagConversationTurn("旧问题 3", longAnswer),
                new RagConversationTurn("旧问题 4", longAnswer),
                new RagConversationTurn("旧问题 5", longAnswer)));

        var promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(chatClient, org.mockito.Mockito.times(2)).prompt(promptCaptor.capture());
        String prompt = promptCaptor.getAllValues().get(1);
        assertTrue(prompt.contains("最近问答上下文"));
        assertTrue(prompt.length() < 12_000);
    }

    @Test
    void appliesOneContextBudgetToSyncAndStreamAndDropsHistoryBeforeReliableSources() {
        String question = "HashMap 的结构是什么";
        MarkdownSearchResult first = new MarkdownSearchResult(
                "HashMap 使用数组。".repeat(12), "java/first.md", "version-1", 0,
                List.of("Java", "集合"), 0.93);
        MarkdownSearchResult second = new MarkdownSearchResult(
                "链表过长时会树化。".repeat(12), "java/second.md", "version-1", 1,
                List.of("Java", "集合"), 0.91);
        MarkdownSearchResult third = new MarkdownSearchResult(
                "这是预算外的第三份资料。".repeat(20), "java/third.md", "version-1", 2,
                List.of("Java", "集合"), 0.89);
        TokenBudget tokenBudget = new TokenBudget();

        MarkdownSimilaritySearchService baselineSearch = mock(MarkdownSimilaritySearchService.class);
        ChatClient baselineChat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore baselineLog = mock(RagRetrievalLogStore.class);
        when(baselineSearch.search(question, 5, 0.60)).thenReturn(List.of(first, second));
        when(baselineChat.prompt(anyString()).call().content())
                .thenReturn("HashMap 使用数组 [资料 1]。");
        clearInvocations(baselineChat);
        new RagAnswerService(baselineSearch, baselineChat, baselineLog).answer(question);
        ArgumentCaptor<String> baselinePrompt = ArgumentCaptor.forClass(String.class);
        verify(baselineChat).prompt(baselinePrompt.capture());
        int inputBudget = tokenBudget.count(baselinePrompt.getValue());

        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        when(searchService.search(question, 5, 0.60)).thenReturn(List.of(first, second, third));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 使用数组 [资料 1]。");
        when(chatClient.prompt(anyString()).stream().content())
                .thenReturn(Flux.just("HashMap 使用数组 [资料 1]。"));
        clearInvocations(chatClient);
        int responseReserve = 100;
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 2,
                new NoopRagAnswerCache(), "test-index", () -> "version-1",
                new NoopRagAnswerCacheObserver(), tokenBudget,
                inputBudget + responseReserve, responseReserve);
        List<RagConversationTurn> history = List.of(
                new RagConversationTurn("应被移除的旧问题", "应被移除的旧回答".repeat(20)));

        RagAnswerResult sync = service.answer(question, history);
        List<RagAnswerStreamEvent> events = service.streamAnswer(question, history).collectList().block();
        RagAnswerStreamEvent streamed = events.get(events.size() - 1);

        assertEquals(List.of(first, second), sync.sources());
        assertEquals(sync.sources(), streamed.sources());
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(chatClient, org.mockito.Mockito.times(2)).prompt(prompts.capture());
        assertEquals(prompts.getAllValues().get(0), prompts.getAllValues().get(1));
        String prompt = prompts.getValue();
        assertTrue(tokenBudget.count(prompt) <= inputBudget);
        assertTrue(prompt.contains("问题：" + question));
        assertTrue(prompt.contains("[资料 1]") && prompt.contains("[资料 2]"));
        assertEquals(false, prompt.contains("[资料 3]"));
        assertEquals(false, prompt.contains("java/third.md"));
        assertEquals(false, prompt.contains("应被移除的旧问题"));
    }

    @Test
    void recordsClientCancellationWithoutMarkingItAsModelFailure() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).stream().content()).thenReturn(Flux.never());
        RagAnswerService service = new RagAnswerService(searchService, chatClient, logStore);

        reactor.core.Disposable subscription = service.streamAnswer("HashMap").subscribe();
        subscription.dispose();

        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.CANCELLED, logCaptor.getValue().status());
        assertEquals("CLIENT_DISCONNECT", logCaptor.getValue().errorType());
    }

    @Test
    void returnsTimeoutResultAndRecordsServerTimeoutForBlockingModelCall() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenAnswer(invocation -> {
            Thread.sleep(200);
            return "HashMap 使用数组 [资料 1]。";
        });
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofMillis(20));

        RagAnswerResult result = service.answer("HashMap");

        assertEquals("知识库回答生成超时，请稍后重试。", result.answer());
        assertEquals(false, result.grounded());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.TIMEOUT, logCaptor.getValue().status());
        assertEquals("SERVER_TIMEOUT", logCaptor.getValue().errorType());
    }

    @Test
    void completesStreamWithTimeoutResultAndRecordsServerTimeout() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).stream().content()).thenReturn(Flux.never());
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofMillis(20));

        List<RagAnswerStreamEvent> events = service.streamAnswer("HashMap").collectList().block();

        RagAnswerStreamEvent complete = events.get(events.size() - 1);
        assertEquals("complete", complete.type());
        assertEquals("知识库回答生成超时，请稍后重试。", complete.content());
        assertEquals(false, complete.grounded());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore).record(logCaptor.capture());
        assertEquals(RagRetrievalLogStatus.TIMEOUT, logCaptor.getValue().status());
        assertEquals("SERVER_TIMEOUT", logCaptor.getValue().errorType());
    }

    @Test
    void rejectsNewAnswerWhenSharedModelConcurrencyIsFull() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(searchService.search("ConcurrentHashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).stream().content()).thenReturn(Flux.never());
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 1);

        reactor.core.Disposable runningStream = service.streamAnswer("HashMap").subscribe();
        RagAnswerResult result = service.answer("ConcurrentHashMap");
        runningStream.dispose();

        assertEquals("当前知识问答较繁忙，请稍后重试。", result.answer());
        assertEquals(false, result.grounded());
        var logCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLog.class);
        verify(logStore, org.mockito.Mockito.times(2)).record(logCaptor.capture());
        RagRetrievalLog overloaded = logCaptor.getAllValues().stream()
                .filter(log -> log.status() == RagRetrievalLogStatus.OVERLOADED)
                .findFirst()
                .orElseThrow();
        assertEquals("CONCURRENCY_LIMIT", overloaded.errorType());
    }

    @Test
    void reusesGroundedAnswerFromCacheWithoutSearchingOrCallingModel() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        RagAnswerResult cachedResult = new RagAnswerResult(
                "HashMap 使用数组 [资料 1]。", true, List.of(source));
        RagAnswerCache cache = new RagAnswerCache() {
            @Override
            public Optional<RagAnswerResult> get(String key) {
                return Optional.of(cachedResult);
            }

            @Override
            public void put(String key, RagAnswerResult result) {
                throw new AssertionError("命中缓存不应回写");
            }
        };
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 1,
                cache, "test-index");

        RagAnswerResult result = service.answer("HashMap");

        assertEquals(cachedResult, result);
        verifyNoInteractions(searchService, chatClient);
        verify(logStore).record(org.mockito.ArgumentMatchers.any(RagRetrievalLog.class));
    }

    @Test
    void bypassesCacheFailureAndStillReturnsFreshGroundedAnswer() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("HashMap 使用数组 [资料 1]。");
        RagAnswerCache cache = new RagAnswerCache() {
            @Override
            public Optional<RagAnswerResult> get(String key) {
                throw new IllegalStateException("Redis 不可用");
            }

            @Override
            public void put(String key, RagAnswerResult result) {
                throw new IllegalStateException("Redis 不可用");
            }
        };
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 1,
                cache, "test-index");

        RagAnswerResult result = service.answer("HashMap");

        assertEquals(true, result.grounded());
        assertEquals("HashMap 使用数组 [资料 1]。", result.answer());
        verify(searchService).search("HashMap", 5, 0.60);
        verify(chatClient, org.mockito.Mockito.times(2)).prompt(anyString());
    }

    @Test
    void changesCacheKeyWhenKnowledgeVersionChanges() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("HashMap 使用数组 [资料 1]。", "HashMap 使用数组 [资料 1]。");
        java.util.Map<String, RagAnswerResult> values = new java.util.HashMap<>();
        RagAnswerCache cache = new RagAnswerCache() {
            @Override
            public Optional<RagAnswerResult> get(String key) {
                return Optional.ofNullable(values.get(key));
            }

            @Override
            public void put(String key, RagAnswerResult result) {
                values.put(key, result);
            }
        };
        java.util.concurrent.atomic.AtomicReference<String> knowledgeVersion =
                new java.util.concurrent.atomic.AtomicReference<>("java/collections.md\u0000version-1");
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 1,
                cache, "test-index", knowledgeVersion::get);

        RagAnswerResult first = service.answer("HashMap");
        knowledgeVersion.set("java/collections.md\u0000version-2");
        RagAnswerResult second = service.answer("HashMap");

        assertEquals("HashMap 使用数组 [资料 1]。", first.answer());
        assertEquals("HashMap 使用数组 [资料 1]。", second.answer());
        verify(searchService, org.mockito.Mockito.times(2)).search("HashMap", 5, 0.60);
        org.junit.jupiter.api.Assertions.assertEquals(2, values.size());
    }

    @Test
    void observesCacheMissAndBypass() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        RagRetrievalLogStore logStore = mock(RagRetrievalLogStore.class);
        RagAnswerCache cache = new RagAnswerCache() {
            private boolean first = true;

            @Override
            public Optional<RagAnswerResult> get(String key) {
                if (first) {
                    first = false;
                    return Optional.empty();
                }
                throw new IllegalStateException("Redis 不可用");
            }

            @Override
            public void put(String key, RagAnswerResult result) {
            }
        };
        RagAnswerCacheObserver observer = mock(RagAnswerCacheObserver.class);
        MarkdownSearchResult source = new MarkdownSearchResult(
                "HashMap 使用数组。", "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
        when(searchService.search("HashMap", 5, 0.60)).thenReturn(List.of(source));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("HashMap 使用数组 [资料 1]。");
        RagAnswerService service = new RagAnswerService(searchService, null, null,
                chatClient, logStore, Duration.ofSeconds(1), Runnable::run, 1,
                cache, "test-index", () -> "version-1", observer);

        service.answer("HashMap");
        service.answer("HashMap");

        org.mockito.Mockito.verify(observer).miss();
        org.mockito.Mockito.verify(observer).bypass();
    }
}
