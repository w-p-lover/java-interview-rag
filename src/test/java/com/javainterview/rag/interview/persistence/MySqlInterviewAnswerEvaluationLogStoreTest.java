package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlInterviewAnswerEvaluationLogStoreTest {

    @Test
    void mapsEvaluationMetadataWithoutInputContent() {
        InterviewAnswerEvaluationLogMapper mapper = mock(InterviewAnswerEvaluationLogMapper.class);
        MySqlInterviewAnswerEvaluationLogStore store =
                new MySqlInterviewAnswerEvaluationLogStore(mapper);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 31, 9, 0);

        store.record(new InterviewAnswerEvaluationLog(
                null,
                InterviewAnswerEvaluationLog.Mode.REVIEW,
                "prompt-v2",
                "rubric-v1",
                "model-test",
                InterviewAnswerEvaluationLog.Status.SUCCEEDED,
                true,
                82,
                78,
                88,
                83,
                1234,
                null,
                createdAt));

        ArgumentCaptor<InterviewAnswerEvaluationLogEntity> captor =
                ArgumentCaptor.forClass(InterviewAnswerEvaluationLogEntity.class);
        verify(mapper).insert(captor.capture());
        InterviewAnswerEvaluationLogEntity entity = captor.getValue();
        assertEquals("REVIEW", entity.getEvaluationMode());
        assertEquals("SUCCEEDED", entity.getStatus());
        assertEquals(83, entity.getOverallScore());
        assertEquals(1234L, entity.getElapsedMillis());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void readsRecentLogsForTheRequestedMode() {
        InterviewAnswerEvaluationLogMapper mapper = mock(InterviewAnswerEvaluationLogMapper.class);
        MySqlInterviewAnswerEvaluationLogStore store =
                new MySqlInterviewAnswerEvaluationLogStore(mapper);
        InterviewAnswerEvaluationLog log = new InterviewAnswerEvaluationLog(
                9L,
                InterviewAnswerEvaluationLog.Mode.BASELINE,
                "prompt-v2",
                "rubric-v1",
                "model-test",
                InterviewAnswerEvaluationLog.Status.FAILED,
                null,
                null,
                null,
                null,
                null,
                45,
                "IllegalStateException",
                LocalDateTime.of(2026, 8, 2, 9, 0));
        when(mapper.findRecent("BASELINE", 20))
                .thenReturn(java.util.List.of(InterviewAnswerEvaluationLogEntity.from(log)));

        java.util.List<InterviewAnswerEvaluationLog> logs =
                store.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20);

        assertEquals(java.util.List.of(log), logs);
        verify(mapper).findRecent("BASELINE", 20);
    }
}
