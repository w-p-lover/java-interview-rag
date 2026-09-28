package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCaseType;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationMetrics;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagRetrievalExperimentServiceTest {

    @Test
    void resumesOnlyFailedCaseAndReusesSuccessfulContract() {
        RagRetrievalExperimentEvaluator evaluator = mock(RagRetrievalExperimentEvaluator.class);
        RagRetrievalExperimentContractProvider contracts = mock(RagRetrievalExperimentContractProvider.class);
        List<RagEvaluationCase> cases = List.of(evaluationCase("case-1"), evaluationCase("case-2"));
        when(evaluator.cases(RagRetrievalExperimentSplit.DEV)).thenReturn(cases);
        when(contracts.current(RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.DEV)).thenReturn(contract("hybrid-contract", 2));
        when(evaluator.summarize(eq(RagRetrievalExperimentStrategy.HYBRID), any()))
                .thenReturn(report(0.9, 0.8, 0.9, 0.2));
        AtomicBoolean failed = new AtomicBoolean(false);
        when(evaluator.evaluateCase(any(), eq(RagRetrievalExperimentStrategy.HYBRID)))
                .thenAnswer(invocation -> {
                    RagEvaluationCase item = invocation.getArgument(0);
                    if (item.id().equals("case-2") && failed.compareAndSet(false, true)) {
                        throw new IllegalStateException("Qdrant 暂时不可用");
                    }
                    return caseResult(item.id());
                });
        InMemoryStore store = new InMemoryStore();
        RagRetrievalExperimentService service = new RagRetrievalExperimentService(
                evaluator, contracts, store, Runnable::run);

        RagRetrievalExperimentService.RunView failedRun = service.start(
                "Hybrid DEV", RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.DEV);
        RagRetrievalExperimentService.RunView resumed = service.resume(failedRun.run().runId());
        RagRetrievalExperimentService.RunView repeated = service.start(
                "重复提交", RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.DEV);

        assertEquals(RagRetrievalExperimentStatus.PARTIAL_FAILED, failedRun.run().status());
        assertEquals(1, failedRun.succeededCaseCount());
        assertEquals(RagRetrievalExperimentStatus.SUCCEEDED, resumed.run().status());
        assertEquals(2, resumed.succeededCaseCount());
        assertEquals(resumed.run().runId(), repeated.run().runId());
        verify(evaluator, times(1)).evaluateCase(cases.get(0), RagRetrievalExperimentStrategy.HYBRID);
        verify(evaluator, times(2)).evaluateCase(cases.get(1), RagRetrievalExperimentStrategy.HYBRID);
    }

    @Test
    void comparesSameContractRunsAndPersistsHumanRejection() {
        RagRetrievalExperimentEvaluator evaluator = mock(RagRetrievalExperimentEvaluator.class);
        RagRetrievalExperimentContractProvider contracts = mock(RagRetrievalExperimentContractProvider.class);
        List<RagEvaluationCase> cases = List.of(evaluationCase("case-1"));
        when(evaluator.cases(RagRetrievalExperimentSplit.HOLDOUT)).thenReturn(cases);
        when(contracts.current(RagRetrievalExperimentStrategy.SPARSE,
                RagRetrievalExperimentSplit.HOLDOUT)).thenReturn(
                contract("sparse-contract", RagRetrievalExperimentStrategy.SPARSE,
                        RagRetrievalExperimentSplit.HOLDOUT, 1));
        when(contracts.current(RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.HOLDOUT)).thenReturn(
                contract("hybrid-contract", RagRetrievalExperimentStrategy.HYBRID,
                        RagRetrievalExperimentSplit.HOLDOUT, 1));
        when(evaluator.evaluateCase(any(), any())).thenAnswer(invocation ->
                caseResult(((RagEvaluationCase) invocation.getArgument(0)).id()));
        when(evaluator.summarize(eq(RagRetrievalExperimentStrategy.SPARSE), any()))
                .thenReturn(report(0.9, 0.8, 0.9, 0.2));
        when(evaluator.summarize(eq(RagRetrievalExperimentStrategy.HYBRID), any()))
                .thenReturn(report(0.9, 0.7, 0.9, 0.3));
        InMemoryStore store = new InMemoryStore();
        RagRetrievalExperimentService service = new RagRetrievalExperimentService(
                evaluator, contracts, store, Runnable::run);

        String baseline = service.start("Sparse 基线", RagRetrievalExperimentStrategy.SPARSE,
                RagRetrievalExperimentSplit.HOLDOUT).run().runId();
        String candidate = service.start("Hybrid 候选", RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.HOLDOUT).run().runId();

        RagRetrievalExperimentService.Comparison comparison = service.compare(baseline, candidate);
        RagRetrievalExperimentService.RunView decided = service.decide(candidate, baseline,
                RagRetrievalExperimentDecision.REJECTED, "MRR 下降且噪声上升");

        assertFalse(comparison.safeguardsPassed());
        assertEquals(List.of("MRR下降", "上下文噪声率上升"), comparison.safeguardFailures());
        assertEquals(RagRetrievalExperimentDecision.REJECTED, decided.run().decision());
        assertEquals(baseline, decided.run().baselineRunId());
        assertEquals("MRR 下降且噪声上升", decided.run().decisionNote());
    }

    @Test
    void rejectsComparisonWhenCorpusContractsDiffer() {
        InMemoryStore store = new InMemoryStore();
        RagRetrievalExperimentEvaluator evaluator = mock(RagRetrievalExperimentEvaluator.class);
        RagRetrievalExperimentContractProvider contracts = mock(RagRetrievalExperimentContractProvider.class);
        RagRetrievalExperimentService service = new RagRetrievalExperimentService(
                evaluator, contracts, store, Runnable::run);
        store.runs.put("baseline", succeededRun("baseline", "corpus-a"));
        store.runs.put("candidate", succeededRun("candidate", "corpus-b"));

        assertThrows(IllegalStateException.class,
                () -> service.compare("baseline", "candidate"));
    }

    private RagRetrievalExperimentContractProvider.Contract contract(String hash, int count) {
        return contract(hash, RagRetrievalExperimentStrategy.HYBRID,
                RagRetrievalExperimentSplit.DEV, count);
    }

    private RagRetrievalExperimentContractProvider.Contract contract(
            String hash, RagRetrievalExperimentStrategy strategy,
            RagRetrievalExperimentSplit split, int count) {
        return new RagRetrievalExperimentContractProvider.Contract(
                "dataset-v1", split, strategy, "corpus", "index", "git", hash, count);
    }

    private RagEvaluationCase evaluationCase(String id) {
        return new RagEvaluationCase(id, "问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("source.md"), List.of(List.of("标题")), List.of("要点"));
    }

    private RagRetrievalEvaluationCaseResult caseResult(String id) {
        return new RagRetrievalEvaluationCaseResult(id, "问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("source.md"), List.of(List.of("标题")), List.of(), List.of(),
                true, 1, true, 1.0, 1.0, 1, 1.0,
                0, 0.0, 0, 0.0, 0, 0.0, false, false, true);
    }

    private RagRetrievalEvaluationReport report(
            double sourceHitRate, double mrr, double rejectionAccuracy, double noiseRate) {
        return new RagRetrievalEvaluationReport(
                5, 0.0, 0.6, 1, 1, 0, 1, sourceHitRate,
                1, 1.0, 1.0, mrr, 1, 1, 1.0,
                0, 0, 0, 0.0, 0, 0, 0.0, 0, 0, 0.0,
                0, rejectionAccuracy, List.of(),
                new RagRetrievalEvaluationMetrics(0.5, 0.5, 0.5, 1.0, noiseRate, 10.0));
    }

    private RagRetrievalExperimentRun succeededRun(String id, String corpusHash) {
        LocalDateTime now = LocalDateTime.now();
        return new RagRetrievalExperimentRun(id, id, RagRetrievalExperimentStrategy.HYBRID,
                "dataset-v1", RagRetrievalExperimentSplit.DEV, corpusHash, "index", "git",
                id + "-contract", RagRetrievalExperimentStatus.SUCCEEDED, 1,
                List.of(RagRetrievalExperimentCaseResult.succeeded(caseResult("case-1"))),
                report(1.0, 1.0, 1.0, 0.0), null, null, null, null,
                now, now, now, null);
    }

    private static final class InMemoryStore implements RagRetrievalExperimentStore {
        private final Map<String, RagRetrievalExperimentRun> runs = new LinkedHashMap<>();

        @Override
        public synchronized RagRetrievalExperimentRun createOrFind(RagRetrievalExperimentRun run) {
            Optional<RagRetrievalExperimentRun> existing = runs.values().stream()
                    .filter(item -> item.contractHash().equals(run.contractHash()))
                    .findFirst();
            existing.ifPresent(value -> { });
            if (existing.isPresent()) return existing.get();
            runs.put(run.runId(), run);
            return run;
        }

        @Override
        public synchronized Optional<RagRetrievalExperimentRun> find(String runId) {
            return Optional.ofNullable(runs.get(runId));
        }

        @Override
        public synchronized List<RagRetrievalExperimentRun> findRecent(int limit) {
            return runs.values().stream().limit(limit).toList();
        }

        @Override
        public List<RagRetrievalExperimentRun> findRecoverable() { return List.of(); }

        @Override
        public synchronized void saveProgress(
                String runId, RagRetrievalExperimentStatus status,
                List<RagRetrievalExperimentCaseResult> caseResults,
                RagRetrievalEvaluationReport report, String errorType, boolean completed) {
            RagRetrievalExperimentRun run = runs.get(runId);
            LocalDateTime now = LocalDateTime.now();
            runs.put(runId, new RagRetrievalExperimentRun(
                    run.runId(), run.name(), run.strategy(), run.datasetVersion(), run.datasetSplit(),
                    run.corpusHash(), run.indexContract(), run.gitSha(), run.contractHash(), status,
                    run.totalCaseCount(), new ArrayList<>(caseResults), report, errorType,
                    run.baselineRunId(), run.decision(), run.decisionNote(), run.createdAt(), now,
                    completed ? now : null, run.decidedAt()));
        }

        @Override
        public synchronized void saveDecision(
                String candidateRunId, String baselineRunId,
                RagRetrievalExperimentDecision decision, String note) {
            RagRetrievalExperimentRun run = runs.get(candidateRunId);
            LocalDateTime now = LocalDateTime.now();
            runs.put(candidateRunId, new RagRetrievalExperimentRun(
                    run.runId(), run.name(), run.strategy(), run.datasetVersion(), run.datasetSplit(),
                    run.corpusHash(), run.indexContract(), run.gitSha(), run.contractHash(), run.status(),
                    run.totalCaseCount(), run.caseResults(), run.report(), run.errorType(), baselineRunId,
                    decision, note, run.createdAt(), now, run.completedAt(), now));
        }
    }
}
