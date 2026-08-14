package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlResumeEvidenceAuditRequestLogStoreTest {

    @Test
    void mapsMetadataWithoutResumePromptOrAuditBody() {
        ResumeEvidenceAuditRequestLogMapper mapper = mock(ResumeEvidenceAuditRequestLogMapper.class);
        MySqlResumeEvidenceAuditRequestLogStore store =
                new MySqlResumeEvidenceAuditRequestLogStore(mapper);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 14, 10, 30);

        store.record(new ResumeEvidenceAuditRequestLog(
                null,
                "request-1",
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v1",
                "qwen3.7-plus",
                ResumeEvidenceAuditRequestLog.Status.SUCCEEDED,
                120,
                30,
                150,
                800,
                null,
                createdAt));

        ArgumentCaptor<ResumeEvidenceAuditRequestLogEntity> captor =
                ArgumentCaptor.forClass(ResumeEvidenceAuditRequestLogEntity.class);
        verify(mapper).insert(captor.capture());
        ResumeEvidenceAuditRequestLogEntity entity = captor.getValue();
        assertEquals("request-1", entity.getRequestId());
        assertEquals("RESUME_EVIDENCE_AUDITOR", entity.getRoleName());
        assertEquals("resume-evidence-audit-v1", entity.getPromptVersion());
        assertEquals("qwen3.7-plus", entity.getModelName());
        assertEquals("SUCCEEDED", entity.getStatus());
        assertEquals(150, entity.getTotalTokens());
        assertEquals(800L, entity.getElapsedMillis());
        assertEquals(createdAt, entity.getCreatedAt());

        List<String> fieldNames = Arrays.stream(
                        ResumeEvidenceAuditRequestLogEntity.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();
        assertFalse(fieldNames.contains("resumeText"));
        assertFalse(fieldNames.contains("prompt"));
        assertFalse(fieldNames.contains("result"));
        assertFalse(fieldNames.contains("responseBody"));
    }

    @Test
    void readsRecentMetadataLogs() {
        ResumeEvidenceAuditRequestLogMapper mapper = mock(ResumeEvidenceAuditRequestLogMapper.class);
        MySqlResumeEvidenceAuditRequestLogStore store =
                new MySqlResumeEvidenceAuditRequestLogStore(mapper);
        ResumeEvidenceAuditRequestLog log = failedLog();
        when(mapper.findRecent(20)).thenReturn(List.of(
                ResumeEvidenceAuditRequestLogEntity.from(log)));

        List<ResumeEvidenceAuditRequestLog> result = store.findRecent(20);

        assertEquals(List.of(log), result);
        verify(mapper).findRecent(20);
    }

    private ResumeEvidenceAuditRequestLog failedLog() {
        return new ResumeEvidenceAuditRequestLog(
                2L,
                "request-2",
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v1",
                "qwen3.7-plus",
                ResumeEvidenceAuditRequestLog.Status.FAILED,
                null,
                null,
                null,
                25,
                "IllegalStateException",
                LocalDateTime.of(2026, 8, 14, 10, 35));
    }
}
