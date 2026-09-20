package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuestionBankServiceTest {
    @Test
    void rejectsSessionWhenFixedFollowUpCapacityIsInsufficient() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        BankSources sources = new BankSources(registry, 600);
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        when(registry.findVersionsBySource("java.md")).thenReturn(List.of(
                new com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary(
                        "java.md", "v1", 4, LocalDateTime.now(),
                        new com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline(1, 1, List.of()))));
        when(registry.findSnapshot("java.md", "v1")).thenReturn(Optional.of(snapshot()));
        when(sessionStore.findByRequestId("bank:req-1")).thenReturn(Optional.empty());
        when(store.listQuestions("java.md")).thenReturn(List.of(question("q-1", List.of())));

        QuestionBankService service = new QuestionBankService(
                store, sources, mock(InterviewSessionService.class), sessionStore, "test");

        assertThrows(RuntimeException.class, () -> service.startSession(new QuestionBankService.SessionRequest(
                "req-1", "java.md", List.of(), 3, 1, InterviewSkill.JAVA_CORE,
                InterviewDifficulty.BASIC, null, null)));
    }

    @Test
    void rejectsFormalSessionWithFewerThanThreeMainQuestions() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        when(sessionStore.findByRequestId("bank:req-small")).thenReturn(Optional.empty());
        QuestionBankService service = new QuestionBankService(
                store, new BankSources(registry, 600), sessionService, sessionStore, "test");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.startSession(new QuestionBankService.SessionRequest(
                        "req-small", "java.md", List.of(), 2, 0, InterviewSkill.JAVA_CORE,
                        InterviewDifficulty.BASIC, null, null)));

        assertEquals("主问题数必须在 3 到 8 之间，固定追问数在 0 到 2 之间", exception.getMessage());
        verifyNoInteractions(sessionService);
    }

    @Test
    void createsFormalSessionFromReviewedQuestionSnapshotsAndRecordsUsage() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        BankSources sources = new BankSources(registry, 600);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        List<BankQuestion> selected = List.of(
                question("q-1", List.of()), question("q-2", List.of()), question("q-3", List.of()));
        when(registry.findVersionsBySource("java.md")).thenReturn(List.of(
                new com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary(
                        "java.md", "v1", 4, LocalDateTime.now(),
                        new com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline(1, 1, List.of()))));
        when(registry.findSnapshot("java.md", "v1")).thenReturn(Optional.of(snapshot()));
        when(sessionStore.findByRequestId("bank:req-2")).thenReturn(Optional.empty());
        selected.forEach(question -> when(store.findQuestion(question.id())).thenReturn(Optional.of(question)));
        when(sessionService.start(eq("知识资料专项面试"), any(), anyList(), eq(3), any(), eq("bank:req-2")))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<com.javainterview.rag.interview.question.InterviewQuestion> questions = invocation.getArgument(2);
                    var session = mock(com.javainterview.rag.interview.session.InterviewSession.class);
                    when(session.id()).thenReturn("session-1");
                    when(session.questions()).thenReturn(questions);
                    return session;
                });
        QuestionBankService service = new QuestionBankService(
                store, sources, sessionService, sessionStore, "test");

        var result = service.startSession(new QuestionBankService.SessionRequest(
                "req-2", "java.md", List.of("q-1", "q-2", "q-3"), 3, 0,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC, "Java", null));

        assertEquals("session-1", result.id());
        verify(sessionService).start(eq("知识资料专项面试"), any(), anyList(), eq(3), any(), eq("bank:req-2"));
        verify(store).recordUsage("session-1", "q-1", 0);
        verify(store).recordUsage("session-1", "q-2", 1);
        verify(store).recordUsage("session-1", "q-3", 2);
    }

    @Test
    void createsFocusedRandomSessionFromSkillCategoryQuotas() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        BankSources sources = new BankSources(registry, 600);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        when(registry.findVersionsBySource("java.md")).thenReturn(List.of(
                new com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary(
                        "java.md", "v1", 4, LocalDateTime.now(),
                        new com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline(1, 1, List.of()))));
        when(registry.findSnapshot("java.md", "v1")).thenReturn(Optional.of(snapshot()));
        when(sessionStore.findByRequestId("bank:req-focused")).thenReturn(Optional.empty());
        when(store.listQuestions("java.md")).thenReturn(List.of(
                question("q-tech", InterviewSkill.RAG_AI, InterviewDifficulty.INTERMEDIATE,
                        "检索生成正确性"),
                question("q-boundary-1", InterviewSkill.RAG_AI, InterviewDifficulty.INTERMEDIATE,
                        "边界、评估与恢复"),
                question("q-boundary-2", InterviewSkill.RAG_AI, InterviewDifficulty.INTERMEDIATE,
                        "边界、评估与恢复"),
                question("q-evidence", InterviewSkill.RAG_AI, InterviewDifficulty.INTERMEDIATE,
                        "证据与取舍表达")));
        when(sessionService.start(eq("知识资料专项面试"), any(), anyList(), eq(3), any(),
                eq("bank:req-focused"))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<com.javainterview.rag.interview.question.InterviewQuestion> questions = invocation.getArgument(2);
            var session = mock(com.javainterview.rag.interview.session.InterviewSession.class);
            when(session.id()).thenReturn("session-focused");
            when(session.questions()).thenReturn(questions);
            return session;
        });
        QuestionBankService service = new QuestionBankService(
                store, sources, sessionService, sessionStore, "test");

        var session = service.startSession(new QuestionBankService.SessionRequest(
                "req-focused", "java.md", List.of(), 3, 0,
                InterviewSkill.RAG_AI, InterviewDifficulty.INTERMEDIATE, null,
                "边界、评估与恢复"));

        assertEquals("session-focused", session.id());
        Map<String, Long> selectedCategories = session.questions().stream()
                .map(question -> question.bankContext().questionId())
                .map(id -> id.startsWith("q-boundary") ? "边界、评估与恢复" : "检索生成正确性")
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        assertEquals(Map.of("检索生成正确性", 1L, "边界、评估与恢复", 2L),
                selectedCategories);
    }

    @Test
    void snapshotsTrainingDimensionForFixedFollowUps() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        BankSources sources = new BankSources(registry, 600);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        when(registry.findVersionsBySource("java.md")).thenReturn(List.of(
                new com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary(
                        "java.md", "v1", 4, LocalDateTime.now(),
                        new com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline(1, 1, List.of()))));
        when(registry.findSnapshot("java.md", "v1")).thenReturn(Optional.of(snapshot()));
        when(sessionStore.findByRequestId("bank:req-follow-up-focus")).thenReturn(Optional.empty());
        when(store.listQuestions("java.md")).thenReturn(List.of(
                question("q-principle", InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC,
                        "原理与实现", List.of(followUp("原理"))),
                question("q-boundary-1", InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC,
                        "并发与边界", List.of(followUp("边界一"))),
                question("q-boundary-2", InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC,
                        "并发与边界", List.of(followUp("边界二"))),
                question("q-expression", InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC,
                        "结论与推理表达", List.of(followUp("表达")))));
        when(sessionService.start(eq("知识资料专项面试"), any(), anyList(), eq(6), any(),
                eq("bank:req-follow-up-focus"))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<com.javainterview.rag.interview.question.InterviewQuestion> questions = invocation.getArgument(2);
            var session = mock(com.javainterview.rag.interview.session.InterviewSession.class);
            when(session.id()).thenReturn("session-follow-up-focus");
            when(session.questions()).thenReturn(questions);
            return session;
        });
        QuestionBankService service = new QuestionBankService(
                store, sources, sessionService, sessionStore, "test");

        var session = service.startSession(new QuestionBankService.SessionRequest(
                "req-follow-up-focus", "java.md", List.of(), 3, 1,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC, null, "并发与边界"));

        var fixedFollowUps = session.questions().stream()
                .filter(question -> question.bankContext().fixedFollowUp())
                .toList();
        assertEquals(3, fixedFollowUps.size());
        assertTrue(fixedFollowUps.stream().allMatch(question ->
                question.bankContext().trainingDimension().equals(
                        session.questions().get(question.bankContext().parentQuestionIndex())
                                .bankContext().trainingDimension())));
        assertTrue(fixedFollowUps.stream().allMatch(question ->
                question.objective().contains("固定追问维度：")));
    }

    private KnowledgeDocumentSnapshot snapshot() {
        MarkdownDocument document = MarkdownDocument.create("java.md", "# Java\n\n正文");
        return new KnowledgeDocumentSnapshot(document, List.of(
                new MarkdownSection("java.md", 0, 1, List.of("Java"), "正文")));
    }

    private BankQuestion question(String id, List<BankQuestion.FollowUp> followUps) {
        return question(id, InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC, "Java", followUps);
    }

    private BankQuestion question(String id, InterviewSkill skill,
                                  InterviewDifficulty difficulty, String category) {
        return question(id, skill, difficulty, category, List.of());
    }

    private BankQuestion question(String id, InterviewSkill skill,
                                  InterviewDifficulty difficulty, String category,
                                  List<BankQuestion.FollowUp> followUps) {
        MarkdownSearchResult evidence = new MarkdownSearchResult("正文", "java.md", "v1", 0, List.of("Java"), null);
        return new BankQuestion(id, null, "java.md", "v1", skill,
                difficulty, category, "题目 " + id, "目标", "答案", List.of("要点"),
                "评分", followUps, List.of(evidence), BankQuestion.Status.ACTIVE, 0,
                0, null, LocalDateTime.now(), LocalDateTime.now());
    }

    private BankQuestion.FollowUp followUp(String label) {
        return new BankQuestion.FollowUp(
                label + "失败时如何恢复？", "记录状态后重试",
                List.of("失败状态", "幂等重试"), "按恢复边界评分");
    }
}
