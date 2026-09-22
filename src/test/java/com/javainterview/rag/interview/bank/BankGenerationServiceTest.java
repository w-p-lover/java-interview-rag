package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankGenerationServiceTest {

    private final ScheduledThreadPoolExecutor recoveryScheduler = new ScheduledThreadPoolExecutor(1);

    @AfterEach
    void tearDown() {
        recoveryScheduler.shutdownNow();
    }

    @Test
    void keepsSuccessfulQuestionsAndResumesOnlyTheMissingCount() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        BankSources sources = mock(BankSources.class);
        BankQuestionGenerator generator = mock(BankQuestionGenerator.class);
        KnowledgeDocumentSnapshot snapshot = snapshot();
        String version = snapshot.document().version();
        MarkdownSearchResult evidence = new MarkdownSearchResult(
                "Java 正文证据", "java.md", version, 0, List.of("Java"), null);
        AtomicReference<BankGenerationTask> taskState = new AtomicReference<>();
        List<BankQuestion> savedQuestions = new ArrayList<>();

        when(sources.currentSnapshot("java.md")).thenReturn(snapshot);
        when(sources.evidence("java.md", version)).thenReturn(List.of(evidence));
        when(sources.isCurrent("java.md", version)).thenReturn(true);
        when(store.findTaskByRequestId("request-1")).thenAnswer(invocation ->
                Optional.ofNullable(taskState.get()).filter(task -> task.requestId().equals("request-1")));
        when(store.findActiveTask("java.md", version)).thenAnswer(invocation ->
                Optional.ofNullable(taskState.get()).filter(task ->
                        task.status() == BankGenerationTask.Status.QUEUED
                                || task.status() == BankGenerationTask.Status.PROCESSING));
        when(store.createOrFindTask(any())).thenAnswer(invocation -> {
            BankGenerationTask task = invocation.getArgument(0);
            taskState.set(task);
            return task;
        });
        when(store.findTask(any())).thenAnswer(invocation -> Optional.ofNullable(taskState.get()));
        when(store.updateTask(any(), anyLong())).thenAnswer(invocation -> {
            BankGenerationTask next = invocation.getArgument(0);
            long expectedVersion = invocation.getArgument(1);
            BankGenerationTask current = taskState.get();
            if (current == null || current.version() != expectedVersion) {
                return false;
            }
            taskState.set(next);
            return true;
        });
        when(store.listQuestions("java.md")).thenAnswer(invocation -> List.copyOf(savedQuestions));
        when(store.saveGenerated(any(), any())).thenAnswer(invocation -> {
            BankQuestion question = invocation.getArgument(0);
            BankGenerationTask expected = invocation.getArgument(1);
            BankGenerationTask current = taskState.get();
            if (current.version() != expected.version()
                    || current.status() != BankGenerationTask.Status.PROCESSING) {
                return false;
            }
            savedQuestions.add(question);
            taskState.set(current.progress(BankGenerationTask.Status.PROCESSING,
                    current.savedCount() + 1, current.skippedCount(), current.attemptId(), null));
            return true;
        });
        when(generator.generate(any(), any(), anyMap(), any())).thenReturn(
                new BankQuestionGenerator.GeneratedSet(List.of(
                        generated("原理与实现", "题目一", List.of(0), "正文证据"),
                        generated("未配置分类", "分类错误题", List.of(0), "正文证据"))),
                new BankQuestionGenerator.GeneratedSet(List.of(
                        generated("并发与边界", "题目二", List.of(0), "正文证据"))));
        BankGenerationService service = service(store, sources, generator, Runnable::run);

        BankGenerationTask first = service.submit("request-1", "java.md", InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE, 2, 0);

        assertEquals(BankGenerationTask.Status.FAILED, first.status());
        assertEquals(1, first.savedCount());
        assertEquals(List.of("题目一"), savedQuestions.stream().map(BankQuestion::question).toList());

        BankGenerationTask resumed = service.resume(first.id());

        assertEquals(BankGenerationTask.Status.COMPLETED, resumed.status());
        assertEquals(2, resumed.savedCount());
        assertEquals(List.of("题目一", "题目二"),
                savedQuestions.stream().map(BankQuestion::question).toList());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> quotas = ArgumentCaptor.forClass(Map.class);
        verify(generator, org.mockito.Mockito.times(2))
                .generate(any(), any(), quotas.capture(), any());
        assertEquals(Map.of("原理与实现", 1, "并发与边界", 1),
                quotas.getAllValues().get(0));
        assertEquals(Map.of("并发与边界", 1), quotas.getAllValues().get(1));
    }

    @Test
    void rejectsAnotherRequestWhileTheSameDocumentVersionIsGenerating() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        BankSources sources = mock(BankSources.class);
        KnowledgeDocumentSnapshot snapshot = snapshot();
        String version = snapshot.document().version();
        BankGenerationTask active = task("active-request", version, BankGenerationTask.Status.PROCESSING);
        when(sources.currentSnapshot("java.md")).thenReturn(snapshot);
        when(store.findTaskByRequestId("new-request")).thenReturn(Optional.empty());
        when(store.findActiveTask("java.md", version)).thenReturn(Optional.of(active));
        BankGenerationService service = service(
                store, sources, mock(BankQuestionGenerator.class), Runnable::run);

        InterviewSessionConflictException exception = assertThrows(
                InterviewSessionConflictException.class,
                () -> service.submit("new-request", "java.md", InterviewSkill.JAVA_CORE,
                        InterviewDifficulty.INTERMEDIATE, 3, 0));

        assertEquals("当前资料版本已有题目生成任务正在执行，请等待完成后再提交", exception.getMessage());
        verify(store, never()).createOrFindTask(any());
    }

    @Test
    void recoversStaleAttemptOnceThenFailsAfterRecoveryBudgetIsExhausted() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        AtomicReference<BankGenerationTask> taskState = new AtomicReference<>(staleTask(0, 4));
        List<Runnable> dispatched = new ArrayList<>();
        when(store.unfinishedTasks()).thenAnswer(invocation -> List.of(taskState.get()));
        when(store.recoverStale(any(), anyLong(), any(), any())).thenAnswer(invocation -> {
            BankGenerationTask replacement = invocation.getArgument(0);
            taskState.set(replacement);
            return true;
        });
        BankGenerationService service = service(store, mock(BankSources.class),
                mock(BankQuestionGenerator.class), dispatched::add);

        service.scanStaleTasks();

        assertEquals(BankGenerationTask.Status.QUEUED, taskState.get().status());
        assertEquals(1, taskState.get().recoveryCount());
        assertEquals(1, dispatched.size());

        taskState.set(staleTask(1, taskState.get().version() + 1));
        dispatched.clear();
        service.scanStaleTasks();

        assertEquals(BankGenerationTask.Status.FAILED, taskState.get().status());
        assertEquals("RECOVERY_EXHAUSTED", taskState.get().errorCode());
        assertEquals(0, dispatched.size());
    }

    private KnowledgeDocumentSnapshot snapshot() {
        MarkdownDocument document = MarkdownDocument.create("java.md", "# Java\n\nJava 正文证据");
        return new KnowledgeDocumentSnapshot(document, List.of(
                new MarkdownSection("java.md", 0, 1, List.of("Java"), "Java 正文证据")));
    }

    private BankGenerationTask task(String requestId, String version, BankGenerationTask.Status status) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return new BankGenerationTask("task-1", requestId, "java.md", version,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE, 3, 0,
                status, 0, 0, status == BankGenerationTask.Status.PROCESSING ? "attempt-1" : null,
                status == BankGenerationTask.Status.PROCESSING ? "worker-1" : null,
                status == BankGenerationTask.Status.PROCESSING ? now : null,
                status == BankGenerationTask.Status.PROCESSING ? now : null, 0,
                BankQuestionGenerator.PROMPT_VERSION, "test-model", null, 0, now, now);
    }

    private BankGenerationTask staleTask(int recoveryCount, long version) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime stale = now.minusMinutes(1);
        return new BankGenerationTask("task-recovery", "request-recovery", "java.md", "v1",
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE, 3, 0,
                BankGenerationTask.Status.PROCESSING, 1, 0, "attempt-old", "worker-old",
                stale, stale, recoveryCount, BankQuestionGenerator.PROMPT_VERSION,
                "test-model", null, version, now.minusMinutes(2), stale);
    }

    private BankGenerationService service(QuestionBankStore store, BankSources sources,
                                          BankQuestionGenerator generator,
                                          java.util.concurrent.Executor executor) {
        return new BankGenerationService(store, sources, generator, executor, "test-model",
                recoveryScheduler, new BankGenerationRecoveryPolicy(1_000, 2_000, 1));
    }

    private BankQuestionGenerator.GeneratedQuestion generated(
            String category, String question, List<Integer> evidenceOrders, String quote) {
        return new BankQuestionGenerator.GeneratedQuestion(
                category, question, "考察目标", "参考答案", List.of("答案要点"),
                "评分标准", List.of(), evidenceOrders, quote);
    }
}
