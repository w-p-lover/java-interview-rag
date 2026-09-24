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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void snapshotsCurrentDynamicModelContractWhenSubmittingTask() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        BankSources sources = mock(BankSources.class);
        KnowledgeDocumentSnapshot snapshot = snapshot();
        AtomicReference<String> contract = new AtomicReference<>("provider-a/qwen#3");
        AtomicReference<BankGenerationTask> taskState = new AtomicReference<>();
        when(sources.currentSnapshot("java.md")).thenReturn(snapshot);
        when(store.findTaskByRequestId("dynamic-contract")).thenReturn(Optional.empty());
        when(store.findActiveTask("java.md", snapshot.document().version())).thenReturn(Optional.empty());
        when(store.createOrFindTask(any())).thenAnswer(invocation -> {
            BankGenerationTask task = invocation.getArgument(0);
            taskState.set(task);
            return task;
        });
        when(store.findTask(any())).thenAnswer(invocation -> Optional.ofNullable(taskState.get()));
        BankGenerationService service = new BankGenerationService(
                store, sources, mock(BankQuestionGenerator.class), ignored -> { }, contract::get,
                recoveryScheduler, new BankGenerationRecoveryPolicy(1_000, 2_000, 1));

        BankGenerationTask submitted = service.submit(
                "dynamic-contract", "java.md", InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE, 3, 0);
        contract.set("provider-b/qwen#4");

        assertEquals("provider-a/qwen#3", submitted.modelName());
        assertEquals("provider-a/qwen#3", taskState.get().modelName());
    }

    @Test
    void recoversStaleAttemptOnceThenFailsAfterRecoveryBudgetIsExhausted() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        AtomicReference<BankGenerationTask> taskState = new AtomicReference<>(staleTask(0, 4));
        List<Runnable> dispatched = new ArrayList<>();
        when(store.unfinishedTasks()).thenAnswer(invocation -> List.of(taskState.get()));
        when(store.findTask("task-recovery")).thenAnswer(invocation -> Optional.of(taskState.get()));
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

    @Test
    void cancelsQueuedWorkAndReleasesQueueCapacity() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.initialize();
        CountDownLatch occupied = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch replacement = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                occupied.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(occupied.await(2, TimeUnit.SECONDS));
            QuestionBankStore store = mock(QuestionBankStore.class);
            BankSources sources = mock(BankSources.class);
            BankQuestionGenerator generator = mock(BankQuestionGenerator.class);
            KnowledgeDocumentSnapshot snapshot = snapshot();
            AtomicReference<BankGenerationTask> state = new AtomicReference<>();
            when(sources.currentSnapshot("java.md")).thenReturn(snapshot);
            when(store.findTaskByRequestId("cancel-request")).thenReturn(Optional.empty());
            when(store.findActiveTask("java.md", snapshot.document().version())).thenReturn(Optional.empty());
            when(store.createOrFindTask(any())).thenAnswer(invocation -> {
                BankGenerationTask task = invocation.getArgument(0);
                state.set(task);
                return task;
            });
            when(store.findTask(any())).thenAnswer(invocation -> Optional.ofNullable(state.get()));
            when(store.cancelQueued(any(), anyLong())).thenAnswer(invocation -> {
                BankGenerationTask cancelled = invocation.getArgument(0);
                state.set(cancelled);
                return true;
            });
            BankGenerationService service = service(store, sources, generator, executor);
            BankGenerationTask queued = service.submit("cancel-request", "java.md", InterviewSkill.JAVA_CORE,
                    InterviewDifficulty.INTERMEDIATE, 3, 0);

            assertEquals(BankGenerationTask.Status.CANCELLED, service.cancel(queued.id()).status());
            assertEquals(BankGenerationTask.Status.CANCELLED, service.cancel(queued.id()).status());
            executor.execute(replacement::countDown);
            release.countDown();
            assertTrue(replacement.await(2, TimeUnit.SECONDS));
            verify(store, org.mockito.Mockito.times(1)).cancelQueued(any(), anyLong());
            verify(generator, never()).generate(any(), any(), anyMap(), any());
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void rejectsCancellationAfterWorkerClaimedTask() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        when(store.findTask("task-1")).thenReturn(Optional.of(
                task("request-1", "v1", BankGenerationTask.Status.PROCESSING)));
        BankGenerationService service = service(store, mock(BankSources.class),
                mock(BankQuestionGenerator.class), Runnable::run);

        assertThrows(InterviewSessionConflictException.class, () -> service.cancel("task-1"));
        verify(store, never()).cancelQueued(any(), anyLong());
    }

    @Test
    void rejectsCancellationWhenWorkerClaimsBetweenReadAndUpdate() {
        QuestionBankStore store = mock(QuestionBankStore.class);
        BankGenerationTask queued = task("request-1", "v1", BankGenerationTask.Status.QUEUED);
        AtomicReference<BankGenerationTask> state = new AtomicReference<>(queued);
        when(store.findTask("task-1")).thenAnswer(invocation -> Optional.of(state.get()));
        when(store.cancelQueued(any(), anyLong())).thenAnswer(invocation -> {
            state.set(queued.startAttempt("attempt-2", "worker-2", java.time.LocalDateTime.now()));
            return false;
        });
        BankGenerationService service = service(store, mock(BankSources.class),
                mock(BankQuestionGenerator.class), Runnable::run);

        assertThrows(InterviewSessionConflictException.class, () -> service.cancel("task-1"));
        assertEquals(BankGenerationTask.Status.PROCESSING, state.get().status());
        verify(store, org.mockito.Mockito.times(1)).cancelQueued(any(), anyLong());
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
