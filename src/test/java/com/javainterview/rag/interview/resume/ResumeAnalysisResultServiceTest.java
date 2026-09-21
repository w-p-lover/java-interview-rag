package com.javainterview.rag.interview.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeAnalysisResultServiceTest {

    private final ResumeCenterStore resumeCenterStore = mock(ResumeCenterStore.class);
    private final InMemoryResultStore resultStore = new InMemoryResultStore();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ResumeAnalysisResultService service;

    @BeforeEach
    void setUp() {
        when(resumeCenterStore.findVersion("version-1")).thenReturn(Optional.of(version("version-1", 1)));
        when(resumeCenterStore.findVersion("version-2")).thenReturn(Optional.of(version("version-2", 2)));
        service = new ResumeAnalysisResultService(resumeCenterStore, resultStore, objectMapper);
    }

    @Test
    void isolatesResultsByImmutableResumeVersion() {
        service.execute("version-1", ResumeAnalysisType.EVIDENCE_AUDIT,
                Map.of("resume", "v1"), "audit-v1", "model-v1", () -> Map.of("summary", "v1 result"));
        service.execute("version-2", ResumeAnalysisType.EVIDENCE_AUDIT,
                Map.of("resume", "v2"), "audit-v1", "model-v1", () -> Map.of("summary", "v2 result"));

        assertEquals("v1 result", service.findLatest(
                "version-1", ResumeAnalysisType.EVIDENCE_AUDIT).result().get("summary").asText());
        assertEquals("v2 result", service.findLatest(
                "version-2", ResumeAnalysisType.EVIDENCE_AUDIT).result().get("summary").asText());
    }

    @Test
    void failedRetryDoesNotOverwritePreviousSuccessfulResult() {
        service.execute("version-1", ResumeAnalysisType.JOB_MATCH,
                Map.of("jd", "first"), "job-match-v1", "model-v1",
                () -> Map.of("summary", "saved result"));

        assertThrows(IllegalStateException.class, () -> service.execute(
                "version-1", ResumeAnalysisType.JOB_MATCH,
                Map.of("jd", "first"), "job-match-v1", "model-v1",
                () -> {
                    throw new IllegalStateException("模型失败");
                }));

        assertEquals("saved result", service.findLatest(
                "version-1", ResumeAnalysisType.JOB_MATCH).result().get("summary").asText());
    }

    @Test
    void rejectsMissingVersionBeforeRunningAnalysis() {
        AtomicBoolean called = new AtomicBoolean(false);

        assertThrows(NoSuchElementException.class, () -> service.execute(
                "missing", ResumeAnalysisType.EXPRESSION_COACH,
                Map.of("facts", List.of()), "expression-v1", "model-v1",
                () -> {
                    called.set(true);
                    return Map.of();
                }));

        assertFalse(called.get());
    }

    @Test
    void restoresPersistedResultThroughNewServiceInstance() {
        service.execute("version-1", ResumeAnalysisType.PROJECT_DEEP_DIVE,
                Map.of("project", "RAG"), "deep-dive-v1", ResumeAnalysisResultService.LOCAL_MODEL,
                () -> Map.of("questionCount", 4));

        ResumeAnalysisResultService restarted = new ResumeAnalysisResultService(
                resumeCenterStore, resultStore, objectMapper);

        assertEquals(4, restarted.findLatest(
                "version-1", ResumeAnalysisType.PROJECT_DEEP_DIVE)
                .result().get("questionCount").asInt());
    }

    private ResumeVersion version(String id, int number) {
        return new ResumeVersion(
                id, "resume-1", number, String.valueOf(number).repeat(64),
                "resume-v" + number + ".txt", "text/plain", 10,
                null, LocalDateTime.of(2026, 9, 21, 9, number));
    }

    private static final class InMemoryResultStore implements ResumeAnalysisResultStore {

        private final Map<String, ResumeAnalysisResult> results = new LinkedHashMap<>();

        @Override
        public ResumeAnalysisResult saveSuccessful(ResumeAnalysisResult result) {
            String key = result.resumeVersionId() + "|" + result.analysisType()
                    + "|" + result.inputFingerprint();
            ResumeAnalysisResult previous = results.get(key);
            ResumeAnalysisResult saved = previous == null ? result : new ResumeAnalysisResult(
                    previous.id(), result.resumeVersionId(), result.analysisType(),
                    result.inputFingerprint(), result.contractVersion(), result.modelName(),
                    result.resultJson(), previous.createdAt(), result.updatedAt());
            results.put(key, saved);
            return saved;
        }

        @Override
        public Optional<ResumeAnalysisResult> findLatest(
                String resumeVersionId, ResumeAnalysisType analysisType) {
            return results.values().stream()
                    .filter(result -> result.resumeVersionId().equals(resumeVersionId)
                            && result.analysisType() == analysisType)
                    .max(Comparator.comparing(ResumeAnalysisResult::updatedAt));
        }

        @Override
        public List<ResumeAnalysisResult> findAll(String resumeVersionId) {
            List<ResumeAnalysisResult> found = new ArrayList<>();
            results.values().stream()
                    .filter(result -> result.resumeVersionId().equals(resumeVersionId))
                    .sorted(Comparator.comparing(ResumeAnalysisResult::updatedAt).reversed())
                    .forEach(found::add);
            return found;
        }
    }
}
