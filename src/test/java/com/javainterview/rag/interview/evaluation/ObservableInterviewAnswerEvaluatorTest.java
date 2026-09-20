package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewSkill;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservableInterviewAnswerEvaluatorTest {

    @Test
    void recordsSuccessfulBaselineEvaluationWithoutInputContent() {
        InterviewAnswerEvaluator delegate = mock(InterviewAnswerEvaluator.class);
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        when(delegate.promptVersion()).thenReturn("prompt-v2");
        when(delegate.rubricVersion()).thenReturn("rubric-v1");
        when(delegate.modelName()).thenReturn("model-test");
        InterviewAnswerAssessment assessment = new InterviewAnswerAssessment(evaluation(80), null);
        when(delegate.evaluateBaseline(any(), any(), any(), any(), any())).thenReturn(assessment);
        ObservableInterviewAnswerEvaluator evaluator = new ObservableInterviewAnswerEvaluator(delegate, store);

        InterviewAnswerAssessment result = evaluator.evaluateBaseline(
                mock(InterviewPreparationProfile.class), mock(InterviewQuestion.class), "敏感回答正文",
                "run-1", "complete-grounded-answer");

        assertEquals(assessment, result);
        ArgumentCaptor<InterviewAnswerEvaluationLog> captor =
                ArgumentCaptor.forClass(InterviewAnswerEvaluationLog.class);
        verify(store).record(captor.capture());
        InterviewAnswerEvaluationLog log = captor.getValue();
        assertEquals(InterviewAnswerEvaluationLog.Mode.BASELINE, log.mode());
        assertEquals(InterviewAnswerEvaluationLog.Status.SUCCEEDED, log.status());
        assertEquals(80, log.overallScore());
        assertEquals(null, log.errorType());
        assertEquals("prompt-v2", log.promptVersion());
        assertEquals("run-1", log.baselineRunId());
        assertEquals("complete-grounded-answer", log.baselineCaseId());
    }

    @Test
    void recordsFailureAndRethrowsTheOriginalException() {
        InterviewAnswerEvaluator delegate = mock(InterviewAnswerEvaluator.class);
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        when(delegate.promptVersion()).thenReturn("prompt-v2");
        when(delegate.rubricVersion()).thenReturn("rubric-v1");
        when(delegate.modelName()).thenReturn("model-test");
        when(delegate.evaluateForReview(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("模型不可用"));
        ObservableInterviewAnswerEvaluator evaluator = new ObservableInterviewAnswerEvaluator(delegate, store);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> evaluator.evaluateForReview(mock(InterviewPreparationProfile.class),
                        mock(InterviewQuestion.class), "回答", List.of()));

        assertEquals("模型不可用", exception.getMessage());
        ArgumentCaptor<InterviewAnswerEvaluationLog> captor =
                ArgumentCaptor.forClass(InterviewAnswerEvaluationLog.class);
        verify(store).record(captor.capture());
        assertEquals(InterviewAnswerEvaluationLog.Mode.REVIEW, captor.getValue().mode());
        assertEquals(InterviewAnswerEvaluationLog.Status.FAILED, captor.getValue().status());
        assertEquals("IllegalStateException", captor.getValue().errorType());
        assertEquals(null, captor.getValue().overallScore());
    }

    @Test
    void forwardsPersistedReviewRubricAndFocusDimensions() {
        InterviewAnswerEvaluator delegate = mock(InterviewAnswerEvaluator.class);
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        InterviewPreparationProfile profile = mock(InterviewPreparationProfile.class);
        InterviewQuestion question = mock(InterviewQuestion.class);
        InterviewAnswerAssessment assessment = new InterviewAnswerAssessment(evaluation(86), null);
        when(delegate.promptVersion()).thenReturn("prompt-v4");
        when(delegate.rubricVersion()).thenReturn("rubric-v3");
        when(delegate.modelName()).thenReturn("model-test");
        when(delegate.evaluateForReview(eq(profile), eq(question), eq("改进后的回答"), eq(List.of()),
                eq(InterviewSkill.RAG_AI), eq("interview-skill-rubric-v1"),
                eq(List.of("边界、评估与恢复")))).thenReturn(assessment);
        ObservableInterviewAnswerEvaluator evaluator = new ObservableInterviewAnswerEvaluator(delegate, store);

        InterviewAnswerAssessment result = evaluator.evaluateForReview(
                profile, question, "改进后的回答", List.of(), InterviewSkill.RAG_AI,
                "interview-skill-rubric-v1", List.of("边界、评估与恢复"));

        assertEquals(assessment, result);
        verify(delegate).evaluateForReview(profile, question, "改进后的回答", List.of(),
                InterviewSkill.RAG_AI, "interview-skill-rubric-v1", List.of("边界、评估与恢复"));
    }

    @Test
    void doesNotFailEvaluationWhenTheLogStoreIsUnavailable() {
        InterviewAnswerEvaluator delegate = mock(InterviewAnswerEvaluator.class);
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        when(delegate.promptVersion()).thenReturn("prompt-v2");
        when(delegate.rubricVersion()).thenReturn("rubric-v1");
        when(delegate.modelName()).thenReturn("model-test");
        when(delegate.evaluate(any(), any(), any())).thenReturn(
                new InterviewAnswerAssessment(evaluation(75), null));
        org.mockito.Mockito.doThrow(new IllegalStateException("日志表不可用"))
                .when(store).record(any());
        ObservableInterviewAnswerEvaluator evaluator = new ObservableInterviewAnswerEvaluator(delegate, store);

        InterviewAnswerAssessment result = evaluator.evaluate(
                mock(InterviewPreparationProfile.class), mock(InterviewQuestion.class), "回答");

        assertEquals(75, result.evaluation().overallScore());
    }

    private InterviewAnswerEvaluation evaluation(int score) {
        return new InterviewAnswerEvaluation(
                score, score, score, score,
                List.of(), List.of(), "继续保持结构化表达。", true);
    }
}
