package com.javainterview.rag.interview.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FixedInterviewAnswerEvaluationServiceTest {

    @Test
    void evaluatesStableSamplesAgainstExpectedBoundaries() {
        FixedInterviewAnswerEvaluationDataset dataset = new FixedInterviewAnswerEvaluationDataset();
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        when(evaluator.promptVersion()).thenReturn("prompt-v2");
        when(evaluator.rubricVersion()).thenReturn("rubric-v1");
        when(evaluator.modelName()).thenReturn("model-test");
        when(evaluator.evaluateBaseline(any(), any(), anyString())).thenAnswer(invocation -> {
            String answer = invocation.getArgument(2);
            if (answer.isBlank()) {
                throw new IllegalArgumentException("回答不能为空");
            }
            int score = answer.contains("页面开发") ? 20
                    : answer.contains("稳定 chunk ID") ? 90
                    : answer.contains("删除旧版本") ? 60 : 70;
            boolean grounded = !answer.contains("线程泄漏");
            return new InterviewAnswerAssessment(evaluation(score, grounded), null);
        });
        FixedInterviewAnswerEvaluationService service =
                new FixedInterviewAnswerEvaluationService(dataset, evaluator);

        FixedInterviewAnswerEvaluationService.Report report = service.evaluate();

        assertEquals("prompt-v2", report.promptVersion());
        assertEquals(5, report.totalCaseCount());
        assertEquals(5, report.passedCaseCount());
        assertEquals(List.of(
                        "blank-answer",
                        "irrelevant-answer",
                        "partial-grounded-answer",
                        "complete-grounded-answer",
                        "ungrounded-answer"),
                report.cases().stream().map(FixedInterviewAnswerEvaluationService.CaseResult::caseId).toList());
        assertTrue(report.cases().stream().allMatch(FixedInterviewAnswerEvaluationService.CaseResult::passed));
        assertEquals(5, dataset.findAll().stream().map(FixedInterviewAnswerEvaluationDataset.Case::id)
                .distinct().count());
    }

    private InterviewAnswerEvaluation evaluation(int score, boolean grounded) {
        return new InterviewAnswerEvaluation(
                score, score, score, score,
                List.of(), List.of("固定样本遗漏"), "补充关键边界。", grounded);
    }
}
