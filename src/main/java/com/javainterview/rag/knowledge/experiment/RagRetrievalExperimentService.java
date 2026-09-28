package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class RagRetrievalExperimentService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DECISION_NOTE_LENGTH = 500;

    private final RagRetrievalExperimentEvaluator evaluator;
    private final RagRetrievalExperimentContractProvider contractProvider;
    private final RagRetrievalExperimentStore store;
    private final Executor executor;
    private final Set<String> scheduledRuns = ConcurrentHashMap.newKeySet();

    public RagRetrievalExperimentService(
            RagRetrievalExperimentEvaluator evaluator,
            RagRetrievalExperimentContractProvider contractProvider,
            RagRetrievalExperimentStore store,
            Executor executor) {
        this.evaluator = Objects.requireNonNull(evaluator, "检索实验评估器不能为空");
        this.contractProvider = Objects.requireNonNull(contractProvider, "检索实验契约不能为空");
        this.store = Objects.requireNonNull(store, "检索实验存储不能为空");
        this.executor = Objects.requireNonNull(executor, "检索实验执行器不能为空");
    }

    @PostConstruct
    public void recover() {
        store.findRecoverable().forEach(run -> {
            if (currentContract(run)) {
                schedule(run.runId());
            } else {
                store.saveProgress(run.runId(), RagRetrievalExperimentStatus.PARTIAL_FAILED,
                        run.caseResults(), null, "RUN_CONTRACT_CHANGED", true);
            }
        });
    }

    public RunView start(String name,
                         RagRetrievalExperimentStrategy strategy,
                         RagRetrievalExperimentSplit split) {
        String normalizedName = requireLength(name, "实验名称不能为空", MAX_NAME_LENGTH);
        Objects.requireNonNull(strategy, "检索策略不能为空");
        Objects.requireNonNull(split, "数据集切分不能为空");
        RagRetrievalExperimentContractProvider.Contract contract =
                contractProvider.current(strategy, split);
        LocalDateTime now = LocalDateTime.now();
        RagRetrievalExperimentRun created = new RagRetrievalExperimentRun(
                UUID.randomUUID().toString(), normalizedName, strategy,
                contract.datasetVersion(), split, contract.corpusHash(), contract.indexContract(),
                contract.gitSha(), contract.contractHash(), RagRetrievalExperimentStatus.QUEUED,
                contract.totalCaseCount(), List.of(), null, null, null, null, null,
                now, now, null, null);
        RagRetrievalExperimentRun run = store.createOrFind(created);
        if (!run.terminal()) {
            schedule(run.runId());
        }
        return view(store.find(run.runId()).orElse(run));
    }

    public List<RunView> findRecent(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("实验运行数量必须在 1 到 50 之间");
        }
        return store.findRecent(limit).stream().map(this::view).toList();
    }

    public RunView find(String runId) {
        return view(requireRun(runId));
    }

    public RunView resume(String runId) {
        RagRetrievalExperimentRun run = requireRun(runId);
        if (!currentContract(run)) {
            throw new IllegalStateException("实验运行契约已变化，不能在不同语料或索引上续跑");
        }
        if (run.status() != RagRetrievalExperimentStatus.SUCCEEDED) {
            schedule(run.runId());
        }
        return view(store.find(run.runId()).orElse(run));
    }

    public Comparison compare(String baselineRunId, String candidateRunId) {
        RagRetrievalExperimentRun baseline = completedRun(baselineRunId);
        RagRetrievalExperimentRun candidate = completedRun(candidateRunId);
        requireComparable(baseline, candidate);
        List<MetricDelta> metrics = metricDeltas(baseline.report(), candidate.report());
        List<String> safeguards = new ArrayList<>();
        requireNotLower("来源命中率", baseline.report().expectedSourceHitRate(),
                candidate.report().expectedSourceHitRate(), safeguards);
        requireNotLower("MRR", baseline.report().meanReciprocalRank(),
                candidate.report().meanReciprocalRank(), safeguards);
        requireNotLower("拒答准确率", baseline.report().rejectionAccuracy(),
                candidate.report().rejectionAccuracy(), safeguards);
        if (candidate.report().advancedMetrics().contextNoiseRate()
                > baseline.report().advancedMetrics().contextNoiseRate()) {
            safeguards.add("上下文噪声率上升");
        }
        return new Comparison(view(baseline), view(candidate), metrics,
                safeguards.isEmpty(), safeguards);
    }

    public RunView decide(String candidateRunId,
                          String baselineRunId,
                          RagRetrievalExperimentDecision decision,
                          String note) {
        Objects.requireNonNull(decision, "采用结论不能为空");
        compare(baselineRunId, candidateRunId);
        String normalizedNote = note == null ? "" : note.strip();
        if (normalizedNote.length() > MAX_DECISION_NOTE_LENGTH) {
            throw new IllegalArgumentException("采用结论说明不能超过 500 个字符");
        }
        store.saveDecision(candidateRunId.strip(), baselineRunId.strip(), decision, normalizedNote);
        return find(candidateRunId);
    }

    private void schedule(String runId) {
        if (!scheduledRuns.add(runId)) {
            return;
        }
        try {
            executor.execute(() -> execute(runId));
        } catch (RuntimeException exception) {
            scheduledRuns.remove(runId);
            throw exception;
        }
    }

    private void execute(String runId) {
        try {
            RagRetrievalExperimentRun run = requireRun(runId);
            if (!currentContract(run)) {
                store.saveProgress(runId, RagRetrievalExperimentStatus.PARTIAL_FAILED,
                        run.caseResults(), null, "RUN_CONTRACT_CHANGED", true);
                return;
            }
            Map<String, RagRetrievalExperimentCaseResult> resultsByCase = new HashMap<>();
            run.caseResults().forEach(result -> resultsByCase.put(result.caseId(), result));
            store.saveProgress(runId, RagRetrievalExperimentStatus.RUNNING,
                    orderedResults(run.datasetSplit(), resultsByCase), null, null, false);

            for (RagEvaluationCase evaluationCase : evaluator.cases(run.datasetSplit())) {
                RagRetrievalExperimentCaseResult existing = resultsByCase.get(evaluationCase.id());
                if (existing != null
                        && existing.status() == RagRetrievalExperimentCaseResult.Status.SUCCEEDED) {
                    continue;
                }
                try {
                    resultsByCase.put(evaluationCase.id(), RagRetrievalExperimentCaseResult.succeeded(
                            evaluator.evaluateCase(evaluationCase, run.strategy())));
                    store.saveProgress(runId, RagRetrievalExperimentStatus.RUNNING,
                            orderedResults(run.datasetSplit(), resultsByCase), null, null, false);
                } catch (RuntimeException exception) {
                    resultsByCase.put(evaluationCase.id(),
                            RagRetrievalExperimentCaseResult.failed(evaluationCase.id(), exception));
                    store.saveProgress(runId, RagRetrievalExperimentStatus.PARTIAL_FAILED,
                            orderedResults(run.datasetSplit(), resultsByCase), null,
                            exception.getClass().getSimpleName(), true);
                    return;
                }
            }

            List<RagRetrievalExperimentCaseResult> caseResults =
                    orderedResults(run.datasetSplit(), resultsByCase);
            List<com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult>
                    successfulResults = caseResults.stream()
                    .filter(result -> result.status()
                            == RagRetrievalExperimentCaseResult.Status.SUCCEEDED)
                    .map(RagRetrievalExperimentCaseResult::result)
                    .toList();
            RagRetrievalEvaluationReport report = evaluator.summarize(
                    run.strategy(), successfulResults);
            store.saveProgress(runId, RagRetrievalExperimentStatus.SUCCEEDED,
                    caseResults, report, null, true);
        } catch (RuntimeException exception) {
            RagRetrievalExperimentRun current = store.find(runId).orElse(null);
            if (current != null) {
                store.saveProgress(runId, RagRetrievalExperimentStatus.PARTIAL_FAILED,
                        current.caseResults(), null, exception.getClass().getSimpleName(), true);
            }
        } finally {
            scheduledRuns.remove(runId);
        }
    }

    private List<RagRetrievalExperimentCaseResult> orderedResults(
            RagRetrievalExperimentSplit split,
            Map<String, RagRetrievalExperimentCaseResult> resultsByCase) {
        Map<String, Integer> order = new HashMap<>();
        List<RagEvaluationCase> cases = evaluator.cases(split);
        for (int index = 0; index < cases.size(); index++) {
            order.put(cases.get(index).id(), index);
        }
        return resultsByCase.values().stream()
                .sorted(Comparator.comparingInt(result ->
                        order.getOrDefault(result.caseId(), Integer.MAX_VALUE)))
                .toList();
    }

    private RagRetrievalExperimentRun requireRun(String runId) {
        String normalized = requireLength(runId, "实验运行 ID 不能为空", 36);
        return store.find(normalized)
                .orElseThrow(() -> new NoSuchElementException("实验运行不存在"));
    }

    private RagRetrievalExperimentRun completedRun(String runId) {
        RagRetrievalExperimentRun run = requireRun(runId);
        if (run.status() != RagRetrievalExperimentStatus.SUCCEEDED || run.report() == null) {
            throw new IllegalStateException("只能比较已成功完成的实验运行");
        }
        return run;
    }

    private void requireComparable(RagRetrievalExperimentRun baseline,
                                   RagRetrievalExperimentRun candidate) {
        if (baseline.runId().equals(candidate.runId())) {
            throw new IllegalArgumentException("基线和候选不能是同一次运行");
        }
        if (!baseline.datasetVersion().equals(candidate.datasetVersion())
                || baseline.datasetSplit() != candidate.datasetSplit()
                || !baseline.corpusHash().equals(candidate.corpusHash())
                || !baseline.indexContract().equals(candidate.indexContract())) {
            throw new IllegalStateException("基线和候选的数据集、语料或索引契约不一致");
        }
    }

    private boolean currentContract(RagRetrievalExperimentRun run) {
        RagRetrievalExperimentContractProvider.Contract current =
                contractProvider.current(run.strategy(), run.datasetSplit());
        return run.contractHash().equals(current.contractHash())
                && run.totalCaseCount() == current.totalCaseCount();
    }

    private RunView view(RagRetrievalExperimentRun run) {
        int succeeded = (int) run.caseResults().stream()
                .filter(result -> result.status()
                        == RagRetrievalExperimentCaseResult.Status.SUCCEEDED)
                .count();
        int failed = (int) run.caseResults().stream()
                .filter(result -> result.status()
                        == RagRetrievalExperimentCaseResult.Status.FAILED)
                .count();
        return new RunView(run, succeeded, failed,
                Math.max(0, run.totalCaseCount() - succeeded - failed));
    }

    private List<MetricDelta> metricDeltas(RagRetrievalEvaluationReport baseline,
                                           RagRetrievalEvaluationReport candidate) {
        return List.of(
                delta("sourceHitRate", "来源命中率", baseline.expectedSourceHitRate(),
                        candidate.expectedSourceHitRate(), true),
                delta("hitAt1Rate", "Hit@1", baseline.hitAt1Rate(), candidate.hitAt1Rate(), true),
                delta("mrr", "MRR", baseline.meanReciprocalRank(),
                        candidate.meanReciprocalRank(), true),
                delta("rejectionAccuracy", "拒答准确率", baseline.rejectionAccuracy(),
                        candidate.rejectionAccuracy(), true),
                delta("precisionAt5", "Precision@5", baseline.advancedMetrics().precisionAt5(),
                        candidate.advancedMetrics().precisionAt5(), true),
                delta("chunkRecallAt5", "Chunk Recall@5",
                        baseline.advancedMetrics().chunkRecallAt5(),
                        candidate.advancedMetrics().chunkRecallAt5(), true),
                delta("contextNoiseRate", "上下文噪声率",
                        baseline.advancedMetrics().contextNoiseRate(),
                        candidate.advancedMetrics().contextNoiseRate(), false),
                delta("latencyP95Ms", "检索 P95 延迟",
                        baseline.advancedMetrics().retrievalLatencyP95Ms(),
                        candidate.advancedMetrics().retrievalLatencyP95Ms(), false));
    }

    private MetricDelta delta(String code, String label, double baseline,
                              double candidate, boolean higherIsBetter) {
        return new MetricDelta(code, label, baseline, candidate,
                candidate - baseline, higherIsBetter);
    }

    private void requireNotLower(String label, double baseline, double candidate,
                                 List<String> safeguards) {
        if (candidate < baseline) {
            safeguards.add(label + "下降");
        }
    }

    private String requireLength(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message + "，且不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    public record RunView(
            RagRetrievalExperimentRun run,
            int succeededCaseCount,
            int failedCaseCount,
            int pendingCaseCount
    ) {
    }

    public record MetricDelta(
            String code,
            String label,
            double baseline,
            double candidate,
            double delta,
            boolean higherIsBetter
    ) {
    }

    public record Comparison(
            RunView baseline,
            RunView candidate,
            List<MetricDelta> metrics,
            boolean safeguardsPassed,
            List<String> safeguardFailures
    ) {
        public Comparison {
            metrics = List.copyOf(metrics);
            safeguardFailures = List.copyOf(safeguardFailures);
        }
    }
}
