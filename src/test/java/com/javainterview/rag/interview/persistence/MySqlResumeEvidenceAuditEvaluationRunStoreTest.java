package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRun;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlResumeEvidenceAuditEvaluationRunStoreTest {

    @Test
    void reusesContractAndRoundTripsCaseResults() throws Exception {
        ResumeEvidenceAuditEvaluationRunMapper mapper =
                mock(ResumeEvidenceAuditEvaluationRunMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        FixedResumeEvidenceAuditEvaluationService.CaseResult caseResult =
                new FixedResumeEvidenceAuditEvaluationService.CaseResult(
                        "case-1", "SUCCEEDED", true, true,
                        1, 1, 1, 0, 0, 0, 0, 0,
                        1, 1, false, 10, 5, 15, 100L, null, List.of());
        LocalDateTime now = LocalDateTime.of(2026, 9, 17, 9, 0);
        ResumeEvidenceAuditEvaluationRunEntity entity =
                new ResumeEvidenceAuditEvaluationRunEntity(
                        "run-existing",
                        "dataset-v1|ROLE|prompt-v1|model-v1",
                        "dataset-v1", "ROLE", "prompt-v1", "model-v1",
                        "RUNNING", 1,
                        objectMapper.writeValueAsString(List.of(caseResult)),
                        null, now, now, null);
        when(mapper.findByContract(anyString())).thenReturn(entity);
        when(mapper.find("run-existing")).thenReturn(entity);
        when(mapper.saveProgress(anyString(), anyString(), anyString(), nullable(String.class),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1);
        MySqlResumeEvidenceAuditEvaluationRunStore store =
                new MySqlResumeEvidenceAuditEvaluationRunStore(mapper, objectMapper);

        ResumeEvidenceAuditEvaluationRun created = store.createOrFind(
                "run-new", "dataset-v1", "ROLE", "prompt-v1", "model-v1", 1);
        ResumeEvidenceAuditEvaluationRun restored = store.find("run-existing").orElseThrow();
        store.saveProgress("run-existing", ResumeEvidenceAuditEvaluationRun.Status.SUCCEEDED,
                restored.caseResults(), null, true);

        assertEquals("run-existing", created.runId());
        assertEquals(caseResult, restored.caseResults().get(0));
        verify(mapper).insert(
                "run-new", "dataset-v1|ROLE|prompt-v1|model-v1",
                "dataset-v1", "ROLE", "prompt-v1", "model-v1", 1);
    }
}
