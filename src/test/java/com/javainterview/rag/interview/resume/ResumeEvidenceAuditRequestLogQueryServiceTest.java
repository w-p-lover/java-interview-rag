package com.javainterview.rag.interview.resume;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeEvidenceAuditRequestLogQueryServiceTest {

    @Test
    void returnsRecentLogsWithinLimit() {
        ResumeEvidenceAuditRequestLogStore store = mock(ResumeEvidenceAuditRequestLogStore.class);
        List<ResumeEvidenceAuditRequestLog> logs = List.of(succeededLog());
        when(store.findRecent(20)).thenReturn(logs);
        ResumeEvidenceAuditRequestLogQueryService service =
                new ResumeEvidenceAuditRequestLogQueryService(store);

        assertEquals(logs, service.findRecent(20));
        verify(store).findRecent(20);
    }

    @Test
    void rejectsInvalidLimitAndPartialTokenUsage() {
        ResumeEvidenceAuditRequestLogQueryService service =
                new ResumeEvidenceAuditRequestLogQueryService(
                        mock(ResumeEvidenceAuditRequestLogStore.class));

        assertEquals("简历审计请求日志查询数量必须在 1 到 100 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.findRecent(0)).getMessage());
        assertEquals("简历审计请求日志查询数量必须在 1 到 100 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.findRecent(101)).getMessage());
        assertEquals("Token 用量必须同时存在或同时为空",
                assertThrows(IllegalArgumentException.class,
                        () -> new ResumeEvidenceAuditRequestLog(
                                null, "request", "role", "prompt-v1", "model",
                                ResumeEvidenceAuditRequestLog.Status.SUCCEEDED,
                                10, null, null, 1, null, LocalDateTime.now())).getMessage());
    }

    private ResumeEvidenceAuditRequestLog succeededLog() {
        return new ResumeEvidenceAuditRequestLog(
                1L,
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
                LocalDateTime.of(2026, 8, 14, 10, 30));
    }
}
