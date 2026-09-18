package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankGenerationServiceTest {

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
        when(generator.generate(any(), any(), anyInt(), any())).thenReturn(
                new BankQuestionGenerator.GeneratedSet(List.of(
                        generated("题目一", List.of(0), "正文证据"),
                        generated("无证据题", List.of(), ""))),
                new BankQuestionGenerator.GeneratedSet(List.of(
                        generated("题目二", List.of(0), "正文证据"))));
        BankGenerationService service = new BankGenerationService(
                store, sources, generator, Runnable::run, "test-model");

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
        BankGenerationService service = new BankGenerationService(
                store, sources, mock(BankQuestionGenerator.class), Runnable::run, "test-model");

        InterviewSessionConflictException exception = assertThrows(
                InterviewSessionConflictException.class,
                () -> service.submit("new-request", "java.md", InterviewSkill.JAVA_CORE,
                        InterviewDifficulty.INTERMEDIATE, 3, 0));

        assertEquals("当前资料版本已有题目生成任务正在执行，请等待完成后再提交", exception.getMessage());
        verify(store, never()).createOrFindTask(any());
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
                BankQuestionGenerator.PROMPT_VERSION, "test-model", null, 0, now, now);
    }

    private BankQuestionGenerator.GeneratedQuestion generated(
            String question, List<Integer> evidenceOrders, String quote) {
        return new BankQuestionGenerator.GeneratedQuestion(
                "Java", question, "考察目标", "参考答案", List.of("答案要点"),
                "评分标准", List.of(), evidenceOrders, quote);
    }
}
