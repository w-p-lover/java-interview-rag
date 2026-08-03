package com.javainterview.rag.interview.evaluation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewAnswerEvaluationLogQueryServiceTest {

    @Test
    void summarizesBaselineCasesAcrossDistinctRunsAndEvaluationVersions() {
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        List<InterviewAnswerEvaluationLog> logs = List.of(
                succeeded(7L, "run-3", "case-a", "prompt-v2", true, 90, 120),
                failed(6L, "run-2", "case-a", "prompt-v2", "IllegalStateException", 20),
                succeeded(5L, "run-1", "case-a", "prompt-v2", false, 70, 80),
                succeeded(4L, "run-1", "case-a", "prompt-v2", false, 60, 70),
                succeeded(3L, "run-1", "case-b", "prompt-v2", false, 50, 40),
                succeeded(2L, "run-0", "case-a", "prompt-v1", true, 75, 60),
                legacyBaseline(1L));
        when(store.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, null, null, 20)).thenReturn(logs);
        InterviewAnswerEvaluationLogQueryService service =
                new InterviewAnswerEvaluationLogQueryService(store);

        InterviewAnswerEvaluationLogQueryService.Report report =
                service.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, null, null, 20);

        assertEquals(InterviewAnswerEvaluationLog.Mode.BASELINE, report.mode());
        assertEquals(7, report.returnedCount());
        assertEquals(6, report.identifiedBaselineCount());
        assertEquals(1, report.unidentifiedBaselineCount());
        assertEquals(logs, report.logs());
        assertEquals(2, report.versionSummaries().size());
        InterviewAnswerEvaluationLogQueryService.VersionSummary current = report.versionSummaries().get(0);
        assertEquals(5, current.totalCount());
        assertEquals(4, current.succeededCount());
        assertEquals(1, current.failedCount());
        assertEquals(1, current.groundedSucceededCount());
        assertEquals(50, current.minimumOverallScore());
        assertEquals(90, current.maximumOverallScore());
        assertEquals(67.5, current.averageOverallScore());
        assertEquals(66.0, current.averageElapsedMillis());
        assertEquals(120, current.maximumElapsedMillis());
        assertEquals(1, current.failureTypeCounts().get("IllegalStateException"));

        InterviewAnswerEvaluationLogQueryService.BaselineCaseSummary caseSummary =
                report.baselineCaseSummaries().stream()
                        .filter(summary -> summary.baselineCaseId().equals("case-a"))
                        .filter(summary -> summary.promptVersion().equals("prompt-v2"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(4, caseSummary.totalLogCount());
        assertEquals(3, caseSummary.distinctRunCount());
        assertEquals(2, caseSummary.succeededRunCount());
        assertEquals(1, caseSummary.failedRunCount());
        assertEquals(1, caseSummary.groundedSucceededRunCount());
        assertEquals(70, caseSummary.minimumOverallScore());
        assertEquals(90, caseSummary.maximumOverallScore());
        assertEquals(80.0, caseSummary.averageOverallScore());
        assertEquals(20, caseSummary.overallScoreRange());
        assertEquals(100.0, caseSummary.overallScorePopulationVariance());
        assertEquals(73.33, caseSummary.averageElapsedMillis());
        assertEquals(120, caseSummary.maximumElapsedMillis());
        assertEquals(1, caseSummary.failureTypeCounts().get("IllegalStateException"));

        InterviewAnswerEvaluationLogQueryService.BaselineCaseSummary singleScore =
                report.baselineCaseSummaries().stream()
                        .filter(summary -> summary.baselineCaseId().equals("case-b"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(null, singleScore.overallScorePopulationVariance());
        verify(store).findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, null, null, 20);
    }

    @Test
    void rejectsUnboundedQueries() {
        InterviewAnswerEvaluationLogQueryService service =
                new InterviewAnswerEvaluationLogQueryService(mock(InterviewAnswerEvaluationLogStore.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findRecent(
                        InterviewAnswerEvaluationLog.Mode.BASELINE, null, null, 101));

        assertEquals("日志查询数量必须在 1 到 100 之间", exception.getMessage());
    }

    @Test
    void rejectsBaselineFiltersForInterviewLogs() {
        InterviewAnswerEvaluationLogQueryService service =
                new InterviewAnswerEvaluationLogQueryService(mock(InterviewAnswerEvaluationLogStore.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findRecent(
                        InterviewAnswerEvaluationLog.Mode.INTERVIEW, "run-1", null, 20));

        assertEquals("基线运行或样本筛选只支持 BASELINE 模式", exception.getMessage());
    }

    private InterviewAnswerEvaluationLog succeeded(Long id, String runId, String caseId,
                                                    String promptVersion, boolean grounded,
                                                    int score, long elapsedMillis) {
        return new InterviewAnswerEvaluationLog(
                id,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
                runId,
                caseId,
                promptVersion,
                "rubric-v1",
                "model-test",
                InterviewAnswerEvaluationLog.Status.SUCCEEDED,
                grounded,
                score,
                score,
                score,
                score,
                elapsedMillis,
                null,
                LocalDateTime.of(2026, 8, 2, 9, 0));
    }

    private InterviewAnswerEvaluationLog failed(Long id, String runId, String caseId,
                                                 String promptVersion, String errorType,
                                                 long elapsedMillis) {
        return new InterviewAnswerEvaluationLog(
                id,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
                runId,
                caseId,
                promptVersion,
                "rubric-v1",
                "model-test",
                InterviewAnswerEvaluationLog.Status.FAILED,
                null,
                null,
                null,
                null,
                null,
                elapsedMillis,
                errorType,
                LocalDateTime.of(2026, 8, 2, 9, 0));
    }

    private InterviewAnswerEvaluationLog legacyBaseline(Long id) {
        return new InterviewAnswerEvaluationLog(
                id,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
                null,
                null,
                "prompt-v1",
                "rubric-v1",
                "model-test",
                InterviewAnswerEvaluationLog.Status.FAILED,
                null,
                null,
                null,
                null,
                null,
                10,
                "LegacyFailure",
                LocalDateTime.of(2026, 8, 2, 9, 0));
    }
}
