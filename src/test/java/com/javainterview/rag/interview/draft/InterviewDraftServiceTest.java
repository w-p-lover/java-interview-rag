package com.javainterview.rag.interview.draft;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionPlanOutcome;
import com.javainterview.rag.interview.question.InterviewQuestionPlanService;
import com.javainterview.rag.interview.question.InterviewSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewDraftServiceTest {

    @Test
    void reusesIdenticalDraftAndExplicitRegenerateRunsAgain() {
        InterviewPreparationService preparationService = mock(InterviewPreparationService.class);
        InterviewQuestionPlanService planService = mock(InterviewQuestionPlanService.class);
        InterviewPreparationResult preparation = preparation();
        when(planService.modelName()).thenReturn("qwen-test");
        when(preparationService.prepare(anyString(), anyString(), anyString())).thenReturn(preparation);
        when(planService.generatePlan(anyString(), any(), anyInt(), anyInt(), any(), any(), anyString()))
                .thenReturn(new InterviewQuestionPlanOutcome(generation(), null, null));
        InterviewDraftService service = new InterviewDraftService(preparationService, planService);

        InterviewDraftResult first = generate(service, false);
        InterviewDraftResult reused = generate(service, false);
        InterviewDraftResult regenerated = generate(service, true);

        assertFalse(first.reused());
        assertTrue(reused.reused());
        assertEquals(first.draftId(), reused.draftId());
        assertFalse(regenerated.reused());
        verify(preparationService, times(2)).prepare(
                "Java 后端工程师", "Spring Boot 项目", "要求 MySQL");
        verify(planService, times(2)).generatePlan(
                anyString(), any(), anyInt(), anyInt(), any(), any(), anyString());
    }

    @Test
    void removesFailedComputationSoTheSameInputCanRetry() {
        InterviewPreparationService preparationService = mock(InterviewPreparationService.class);
        InterviewQuestionPlanService planService = mock(InterviewQuestionPlanService.class);
        when(planService.modelName()).thenReturn("qwen-test");
        when(preparationService.prepare(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("模型暂不可用"))
                .thenReturn(preparation());
        when(planService.generatePlan(anyString(), any(), anyInt(), anyInt(), any(), any(), anyString()))
                .thenReturn(new InterviewQuestionPlanOutcome(generation(), null, null));
        InterviewDraftService service = new InterviewDraftService(preparationService, planService);

        assertThrows(IllegalStateException.class, () -> generate(service, false));
        InterviewDraftResult retried = generate(service, false);

        assertFalse(retried.reused());
        verify(preparationService, times(2)).prepare(
                "Java 后端工程师", "Spring Boot 项目", "要求 MySQL");
    }

    @Test
    void sharesOneInFlightComputationForConcurrentIdenticalRequests() throws Exception {
        InterviewPreparationService preparationService = mock(InterviewPreparationService.class);
        InterviewQuestionPlanService planService = mock(InterviewQuestionPlanService.class);
        CountDownLatch preparationStarted = new CountDownLatch(1);
        CountDownLatch allowPreparationToFinish = new CountDownLatch(1);
        when(planService.modelName()).thenReturn("qwen-test");
        when(preparationService.prepare(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    preparationStarted.countDown();
                    allowPreparationToFinish.await(2, TimeUnit.SECONDS);
                    return preparation();
                });
        when(planService.generatePlan(anyString(), any(), anyInt(), anyInt(), any(), any(), anyString()))
                .thenReturn(new InterviewQuestionPlanOutcome(generation(), null, null));
        InterviewDraftService service = new InterviewDraftService(preparationService, planService);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<InterviewDraftResult> first = executor.submit(() -> generate(service, false));
            assertTrue(preparationStarted.await(2, TimeUnit.SECONDS));
            Future<InterviewDraftResult> second = executor.submit(() -> generate(service, false));
            allowPreparationToFinish.countDown();

            InterviewDraftResult firstResult = first.get(2, TimeUnit.SECONDS);
            InterviewDraftResult secondResult = second.get(2, TimeUnit.SECONDS);

            assertEquals(firstResult.draftId(), secondResult.draftId());
            assertTrue(firstResult.reused() || secondResult.reused());
            verify(preparationService).prepare(
                    "Java 后端工程师", "Spring Boot 项目", "要求 MySQL");
        } finally {
            allowPreparationToFinish.countDown();
            executor.shutdownNow();
        }
    }

    private InterviewDraftResult generate(InterviewDraftService service, boolean regenerate) {
        return service.generate(
                "Java 后端工程师",
                "Spring Boot 项目",
                "要求 MySQL",
                3,
                8,
                InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE,
                regenerate);
    }

    private InterviewPreparationResult preparation() {
        return new InterviewPreparationResult(
                "Java 后端工程师",
                "interview-preparation-v1",
                new InterviewPreparationProfile(
                        "Java 后端候选人",
                        List.of("Spring Boot 项目"),
                        List.of("要求 MySQL"),
                        List.of(new InterviewFocusArea(
                                "Java",
                                "岗位与简历均涉及 Java",
                                List.of(InterviewFocusSource.RESUME,
                                        InterviewFocusSource.JOB_DESCRIPTION)))));
    }

    private InterviewQuestionGenerationResult generation() {
        InterviewQuestion question = new InterviewQuestion(
                "请介绍项目中的事务边界",
                "验证事务设计",
                List.of(InterviewFocusSource.RESUME),
                "Spring 事务",
                false,
                List.of());
        return new InterviewQuestionGenerationResult(
                "Java 后端工程师",
                "interview-question-v1",
                List.of(question, question, question));
    }
}
