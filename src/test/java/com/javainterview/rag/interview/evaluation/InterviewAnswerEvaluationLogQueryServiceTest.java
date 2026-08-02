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
    void groupsRecentLogsByEvaluationVersionAndSummarizesRecordedFacts() {
        InterviewAnswerEvaluationLogStore store = mock(InterviewAnswerEvaluationLogStore.class);
        List<InterviewAnswerEvaluationLog> logs = List.of(
                succeeded(4L, "prompt-v2", true, 90, 120),
                failed(3L, "prompt-v2", "IllegalArgumentException", 10),
                succeeded(2L, "prompt-v2", false, 60, 50),
                succeeded(1L, "prompt-v1", true, 75, 80));
        when(store.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20)).thenReturn(logs);
        InterviewAnswerEvaluationLogQueryService service =
                new InterviewAnswerEvaluationLogQueryService(store);

        InterviewAnswerEvaluationLogQueryService.Report report =
                service.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20);

        assertEquals(InterviewAnswerEvaluationLog.Mode.BASELINE, report.mode());
        assertEquals(4, report.returnedCount());
        assertEquals(logs, report.logs());
        assertEquals(2, report.versionSummaries().size());
        InterviewAnswerEvaluationLogQueryService.VersionSummary current = report.versionSummaries().get(0);
        assertEquals(3, current.totalCount());
        assertEquals(2, current.succeededCount());
        assertEquals(1, current.failedCount());
        assertEquals(1, current.groundedSucceededCount());
        assertEquals(60, current.minimumOverallScore());
        assertEquals(90, current.maximumOverallScore());
        assertEquals(75.0, current.averageOverallScore());
        assertEquals(60.0, current.averageElapsedMillis());
        assertEquals(120, current.maximumElapsedMillis());
        assertEquals(1, current.failureTypeCounts().get("IllegalArgumentException"));
        verify(store).findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20);
    }

    @Test
    void rejectsUnboundedQueries() {
        InterviewAnswerEvaluationLogQueryService service =
                new InterviewAnswerEvaluationLogQueryService(mock(InterviewAnswerEvaluationLogStore.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 101));

        assertEquals("日志查询数量必须在 1 到 100 之间", exception.getMessage());
    }

    private InterviewAnswerEvaluationLog succeeded(Long id, String promptVersion, boolean grounded,
                                                    int score, long elapsedMillis) {
        return new InterviewAnswerEvaluationLog(
                id,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
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

    private InterviewAnswerEvaluationLog failed(Long id, String promptVersion, String errorType,
                                                 long elapsedMillis) {
        return new InterviewAnswerEvaluationLog(
                id,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
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
}
