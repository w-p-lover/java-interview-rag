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
        when(resumeCenterStore.findVersion("other-version"))
                .thenReturn(Optional.of(version("other-version", "resume-2", 1)));
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

    @Test
    void comparesOnlyConfirmedFactsAcrossVersions() {
        service.execute("version-1", ResumeAnalysisType.EVIDENCE_AUDIT,
                "v1", "audit-v1", "model-v1", () -> audit(
                        List.of(
                                fact(ResumeEvidenceAuditResult.FactType.TECHNOLOGY,
                                        "使用 Spring Boot 3", "Spring Boot 3"),
                                fact(ResumeEvidenceAuditResult.FactType.RESULT,
                                        "接口耗时降低 30%", "接口耗时降低 30%"),
                                fact(ResumeEvidenceAuditResult.FactType.PROJECT,
                                        "负责旧项目", "负责旧项目")),
                        List.of("旧版待补信息")));
        service.execute("version-2", ResumeAnalysisType.EVIDENCE_AUDIT,
                "v2", "audit-v1", "model-v1", () -> audit(
                        List.of(
                                fact(ResumeEvidenceAuditResult.FactType.TECHNOLOGY,
                                        "使用 Spring Boot 3", "Spring Boot 3"),
                                fact(ResumeEvidenceAuditResult.FactType.RESULT,
                                        "接口耗时降低 40%", "接口耗时降低 40%"),
                                fact(ResumeEvidenceAuditResult.FactType.RESPONSIBILITY,
                                        "负责 RAG 评估", "负责 RAG 评估")),
                        List.of("新版不同待补信息")));

        ResumeAnalysisResultService.EvidenceAuditComparison comparison =
                service.compareEvidenceAudits("version-1", "version-2");

        assertEquals(1, comparison.unchangedFacts().size());
        assertEquals("接口耗时降低 30%", comparison.changedFacts().get(0).before().statement());
        assertEquals("接口耗时降低 40%", comparison.changedFacts().get(0).after().statement());
        assertEquals("负责 RAG 评估", comparison.addedFacts().get(0).statement());
        assertEquals("负责旧项目", comparison.removedFacts().get(0).statement());
    }

    @Test
    void rejectsComparingVersionsFromDifferentResumeRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> service.compareEvidenceAudits("version-1", "other-version"));
    }

    private ResumeEvidenceAuditResult audit(
            List<ResumeEvidenceAuditResult.ConfirmedFact> facts,
            List<String> missingInformation) {
        return new ResumeEvidenceAuditResult(
                "request-1", "RESUME_EVIDENCE_AUDITOR", "audit-v1",
                facts, missingInformation, List.of("风险"), List.of("建议"));
    }

    private ResumeEvidenceAuditResult.ConfirmedFact fact(
            ResumeEvidenceAuditResult.FactType type,
            String statement,
            String evidence) {
        return new ResumeEvidenceAuditResult.ConfirmedFact(type, statement, evidence);
    }

    private ResumeVersion version(String id, int number) {
        return version(id, "resume-1", number);
    }

    private ResumeVersion version(String id, String resumeId, int number) {
        return new ResumeVersion(
                id, resumeId, number, String.valueOf(number).repeat(64),
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
