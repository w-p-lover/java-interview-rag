package com.javainterview.rag.interview.evaluation;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class ScoreStabilityService {
    private final FixedInterviewAnswerEvaluationDataset dataset;
    private final InterviewAnswerEvaluator evaluator;
    private final ScoreStabilityContractProvider contracts;
    private final ScoreStabilityStore store;
    private final Executor executor;
    private final Set<String> scheduled = ConcurrentHashMap.newKeySet();

    public ScoreStabilityService(FixedInterviewAnswerEvaluationDataset dataset,
                                 InterviewAnswerEvaluator evaluator,
                                 ScoreStabilityStore store, Executor executor) {
        this.dataset = dataset;
        this.evaluator = evaluator;
        this.contracts = new ScoreStabilityContractProvider(dataset, evaluator);
        this.store = store;
        this.executor = executor;
    }

    @PostConstruct
    public void recover() {
        store.findRecoverable().forEach(run -> {
            if (current(run)) {
                schedule(run.runId());
            } else {
                store.saveProgress(run.runId(), ScoreStabilityStatus.PARTIAL_FAILED,
                        run.caseResults(), null, "RUN_CONTRACT_CHANGED", true);
            }
        });
    }

    public RunView start(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("评分稳定性实验名称不能为空且不能超过 100 个字符");
        }
        ScoreStabilityContractProvider.Contract contract = contracts.current();
        LocalDateTime now = LocalDateTime.now();
        ScoreStabilityRun run = new ScoreStabilityRun(UUID.randomUUID().toString(), name.strip(),
                contract.datasetVersion(), contract.promptVersion(), contract.rubricVersion(),
                contract.modelName(), contract.contractHash(), ScoreStabilityStatus.QUEUED,
                contract.totalCaseCount(), contract.repeatCount(), List.of(), null, null,
                null, null, null, now, now, null, null);
        ScoreStabilityRun actual = store.createOrFind(run);
        if (!actual.terminal()) {
            schedule(actual.runId());
        }
        return view(actual);
    }

    public List<RunView> recent(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("评分稳定性实验运行数量必须在 1 到 50 之间");
        }
        return store.findRecent(limit).stream().map(this::view).toList();
    }

    public RunView find(String id) {
        return view(require(id));
    }

    public RunView resume(String id) {
        ScoreStabilityRun run = require(id);
        if (!current(run)) {
            throw new IllegalStateException("评分稳定性实验契约已变化，不能续跑");
        }
        if (run.status() != ScoreStabilityStatus.SUCCEEDED) {
            schedule(id);
        }
        return find(id);
    }

    public Comparison compare(String baselineId, String candidateId) {
        ScoreStabilityRun baseline = completed(baselineId);
        ScoreStabilityRun candidate = completed(candidateId);
        if (baseline.runId().equals(candidate.runId())
                || !baseline.datasetVersion().equals(candidate.datasetVersion())) {
            throw new IllegalStateException("基线和候选必须使用同一评分稳定性数据集且不能是同一次运行");
        }
        ScoreStabilityReport b = baseline.report();
        ScoreStabilityReport c = candidate.report();
        List<MetricDelta> metrics = List.of(
                delta("passRate", "评分样本通过率", b.passRate(), c.passRate(), true),
                delta("groundedAccuracy", "知识依据判断准确率", b.groundedAccuracy(), c.groundedAccuracy(), true),
                delta("stableCaseRate", "稳定样本比例", b.stableCaseRate(), c.stableCaseRate(), true),
                delta("averageScore", "平均总分", b.averageScore(), c.averageScore(), true),
                delta("averageScoreRange", "平均分数波动范围", b.averageScoreRange(), c.averageScoreRange(), false),
                delta("latencyP95Ms", "评分 P95 延迟", b.latencyP95Ms(), c.latencyP95Ms(), false));
        List<String> failures = new ArrayList<>();
        if (c.groundedAccuracy() < b.groundedAccuracy()) failures.add("知识依据判断准确率下降");
        if (c.stableCaseRate() < b.stableCaseRate()) failures.add("稳定样本比例下降");
        if (c.averageScoreRange() > b.averageScoreRange() + 0.001) failures.add("平均分数波动扩大");
        return new Comparison(view(baseline), view(candidate), metrics, failures.isEmpty(), failures);
    }

    public RunView decide(String candidateId, String baselineId, AnswerExperimentDecision decision, String note) {
        if (decision == null) throw new IllegalArgumentException("评分稳定性实验结论不能为空");
        compare(baselineId, candidateId);
        if (note != null && note.strip().length() > 500) {
            throw new IllegalArgumentException("结论说明不能超过 500 个字符");
        }
        store.saveDecision(candidateId, baselineId, decision, note == null ? "" : note.strip());
        return find(candidateId);
    }

    private void schedule(String id) {
        if (!scheduled.add(id)) return;
        try {
            executor.execute(() -> execute(id));
        } catch (RuntimeException exception) {
            scheduled.remove(id);
            throw exception;
        }
    }

    private void execute(String id) {
        try {
            ScoreStabilityRun run = require(id);
            if (!current(run)) {
                store.saveProgress(id, ScoreStabilityStatus.PARTIAL_FAILED, run.caseResults(),
                        null, "RUN_CONTRACT_CHANGED", true);
                return;
            }
            Map<String, ScoreStabilityCaseResult> results = new HashMap<>();
            run.caseResults().forEach(result -> results.put(result.caseId(), result));
            store.saveProgress(id, ScoreStabilityStatus.RUNNING, ordered(results), null, null, false);
            int repeats = contracts.current().repeatCount();
            for (FixedInterviewAnswerEvaluationDataset.Case evaluationCase : dataset.findAll()) {
                ScoreStabilityCaseResult previous = results.get(evaluationCase.id());
                if (previous != null && previous.complete(repeats)) continue;
                List<Integer> scores = previous == null ? new ArrayList<>() : new ArrayList<>(previous.overallScores());
                List<Boolean> grounded = previous == null ? new ArrayList<>() : new ArrayList<>(previous.knowledgeGroundedResults());
                long started = System.nanoTime();
                try {
                    if (evaluationCase.expectedRejected()) {
                        evaluator.evaluateBaseline(evaluationCase.profile(), evaluationCase.question(),
                                evaluationCase.answer(), id, evaluationCase.id() + "#rejection");
                        throw new IllegalStateException("预期拒绝样本未被拒绝");
                    }
                    while (scores.size() < repeats) {
                        int repeat = scores.size();
                        InterviewAnswerEvaluation evaluation = evaluator.evaluateBaseline(
                                evaluationCase.profile(), evaluationCase.question(), evaluationCase.answer(),
                                id, evaluationCase.id() + "#" + repeat).evaluation();
                        scores.add(evaluation.overallScore());
                        grounded.add(evaluation.knowledgeGrounded());
                        results.put(evaluationCase.id(), new ScoreStabilityCaseResult(
                                evaluationCase.id(), ScoreStabilityCaseResult.Status.SUCCEEDED,
                                passes(evaluationCase, scores, grounded), evaluationCase.expectedKnowledgeGrounded(),
                                scores, grounded, elapsed(started), null));
                        store.saveProgress(id, ScoreStabilityStatus.RUNNING, ordered(results), null, null, false);
                    }
                } catch (RuntimeException exception) {
                    if (evaluationCase.expectedRejected()) {
                        results.put(evaluationCase.id(), new ScoreStabilityCaseResult(evaluationCase.id(),
                                ScoreStabilityCaseResult.Status.SUCCEEDED, true,
                                evaluationCase.expectedKnowledgeGrounded(), List.of(), List.of(), elapsed(started),
                                exception.getClass().getSimpleName()));
                        store.saveProgress(id, ScoreStabilityStatus.RUNNING, ordered(results), null, null, false);
                    } else {
                        results.put(evaluationCase.id(), new ScoreStabilityCaseResult(evaluationCase.id(),
                                ScoreStabilityCaseResult.Status.FAILED, false,
                                evaluationCase.expectedKnowledgeGrounded(), scores, grounded, elapsed(started),
                                exception.getClass().getSimpleName()));
                        store.saveProgress(id, ScoreStabilityStatus.PARTIAL_FAILED, ordered(results), null,
                                exception.getClass().getSimpleName(), true);
                        return;
                    }
                }
            }
            List<ScoreStabilityCaseResult> ordered = ordered(results);
            store.saveProgress(id, ScoreStabilityStatus.SUCCEEDED, ordered,
                    ScoreStabilityReport.from(ordered, repeats), null, true);
        } catch (RuntimeException exception) {
            store.find(id).ifPresent(run -> store.saveProgress(id, ScoreStabilityStatus.PARTIAL_FAILED,
                    run.caseResults(), null, exception.getClass().getSimpleName(), true));
        } finally {
            scheduled.remove(id);
        }
    }

    private boolean passes(FixedInterviewAnswerEvaluationDataset.Case c, List<Integer> scores,
                           List<Boolean> grounded) {
        return scores.stream().allMatch(score -> score >= c.expectedMinOverallScore()
                        && score <= c.expectedMaxOverallScore())
                && grounded.stream().allMatch(actual -> actual == c.expectedKnowledgeGrounded());
    }

    private long elapsed(long started) { return Math.max(0, (System.nanoTime() - started) / 1_000_000); }

    private List<ScoreStabilityCaseResult> ordered(Map<String, ScoreStabilityCaseResult> values) {
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < dataset.findAll().size(); i++) order.put(dataset.findAll().get(i).id(), i);
        return values.values().stream().sorted(Comparator.comparingInt(result ->
                order.getOrDefault(result.caseId(), Integer.MAX_VALUE))).toList();
    }

    private boolean current(ScoreStabilityRun run) {
        ScoreStabilityContractProvider.Contract current = contracts.current();
        return run.contractHash().equals(current.contractHash()) && run.repeatCount() == current.repeatCount();
    }

    private ScoreStabilityRun require(String id) {
        return store.find(id).orElseThrow(() -> new NoSuchElementException("评分稳定性实验不存在"));
    }

    private ScoreStabilityRun completed(String id) {
        ScoreStabilityRun run = require(id);
        if (run.status() != ScoreStabilityStatus.SUCCEEDED || run.report() == null) {
            throw new IllegalStateException("只能比较已成功完成的评分稳定性实验");
        }
        return run;
    }

    private RunView view(ScoreStabilityRun run) {
        int succeeded = (int) run.caseResults().stream()
                .filter(result -> result.status() == ScoreStabilityCaseResult.Status.SUCCEEDED).count();
        int failed = run.caseResults().size() - succeeded;
        return new RunView(run, succeeded, failed,
                Math.max(0, run.totalCaseCount() - succeeded - failed));
    }

    private MetricDelta delta(String code, String label, double baseline, double candidate, boolean higher) {
        return new MetricDelta(code, label, baseline, candidate, candidate - baseline, higher);
    }

    public record RunView(ScoreStabilityRun run, int succeededCaseCount, int failedCaseCount,
                          int pendingCaseCount) { }
    public record MetricDelta(String code, String label, double baseline, double candidate,
                              double delta, boolean higherIsBetter) { }
    public record Comparison(RunView baseline, RunView candidate, List<MetricDelta> metrics,
                             boolean safeguardsPassed, List<String> safeguardFailures) {
        public Comparison {
            metrics = List.copyOf(metrics);
            safeguardFailures = List.copyOf(safeguardFailures);
        }
    }
}
