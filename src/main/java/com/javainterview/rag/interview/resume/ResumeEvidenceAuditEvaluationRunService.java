package com.javainterview.rag.interview.resume;

import jakarta.annotation.PostConstruct;

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

public class ResumeEvidenceAuditEvaluationRunService {

    private final FixedResumeEvidenceAuditEvaluationService evaluationService;
    private final ResumeEvidenceAuditEvaluationRunStore runStore;
    private final Executor runExecutor;
    private final Set<String> scheduledRuns = ConcurrentHashMap.newKeySet();

    public ResumeEvidenceAuditEvaluationRunService(
            FixedResumeEvidenceAuditEvaluationService evaluationService,
            ResumeEvidenceAuditEvaluationRunStore runStore,
            Executor runExecutor) {
        this.evaluationService = Objects.requireNonNull(evaluationService, "简历审计评估服务不能为空");
        this.runStore = Objects.requireNonNull(runStore, "简历审计评估运行存储不能为空");
        this.runExecutor = Objects.requireNonNull(runExecutor, "简历审计评估运行执行器不能为空");
    }

    @PostConstruct
    public void recover() {
        runStore.findRecoverable().forEach(run -> {
            if (currentContract(run)) {
                schedule(run);
            } else {
                runStore.saveProgress(run.runId(),
                        ResumeEvidenceAuditEvaluationRun.Status.PARTIAL_FAILED,
                        run.caseResults(), "RUN_CONTRACT_CHANGED", true);
            }
        });
    }

    public RunView start() {
        ResumeEvidenceAuditEvaluationRun run = runStore.createOrFind(
                UUID.randomUUID().toString(),
                evaluationService.datasetVersion(),
                evaluationService.roleName(),
                evaluationService.promptVersion(),
                evaluationService.modelName(),
                evaluationService.evaluationCases().size());
        if (!run.terminal()) {
            schedule(run);
        }
        return toView(runStore.find(run.runId()).orElse(run));
    }

    public RunView find(String runId) {
        return toView(requireRun(runId));
    }

    public RunView resume(String runId) {
        ResumeEvidenceAuditEvaluationRun run = requireRun(runId);
        if (!currentContract(run)) {
            throw new IllegalStateException("评估运行契约已变更，不能继续调用旧数据集或旧 Prompt");
        }
        if (run.status() != ResumeEvidenceAuditEvaluationRun.Status.SUCCEEDED) {
            schedule(run);
        }
        return toView(runStore.find(run.runId()).orElse(run));
    }

    private ResumeEvidenceAuditEvaluationRun requireRun(String runId) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("简历审计评估运行标识不能为空");
        }
        return runStore.find(runId.strip())
                .orElseThrow(() -> new NoSuchElementException("简历审计评估运行不存在"));
    }

    private void schedule(ResumeEvidenceAuditEvaluationRun run) {
        if (!scheduledRuns.add(run.runId())) {
            return;
        }
        try {
            runExecutor.execute(() -> execute(run.runId()));
        } catch (RuntimeException exception) {
            scheduledRuns.remove(run.runId());
            throw exception;
        }
    }

    private void execute(String runId) {
        try {
            ResumeEvidenceAuditEvaluationRun run = requireRun(runId);
            List<FixedResumeEvidenceAuditEvaluationService.CaseResult> results =
                    new ArrayList<>(run.caseResults());
            runStore.saveProgress(runId, ResumeEvidenceAuditEvaluationRun.Status.RUNNING,
                    results, null, false);

            Map<String, FixedResumeEvidenceAuditEvaluationService.CaseResult> resultsByCase =
                    new HashMap<>();
            results.forEach(result -> resultsByCase.put(result.caseId(), result));
            for (FixedResumeEvidenceAuditDataset.Case evaluationCase
                    : evaluationService.evaluationCases()) {
                FixedResumeEvidenceAuditEvaluationService.CaseResult existing =
                        resultsByCase.get(evaluationCase.id());
                if (existing != null && "SUCCEEDED".equals(existing.status())) {
                    continue;
                }
                FixedResumeEvidenceAuditEvaluationService.CaseResult evaluated =
                        evaluationService.evaluateCase(evaluationCase);
                resultsByCase.put(evaluationCase.id(), evaluated);
                results = orderedResults(resultsByCase);
                runStore.saveProgress(runId, ResumeEvidenceAuditEvaluationRun.Status.RUNNING,
                        results, null, false);
            }

            boolean allSucceeded = results.size() == evaluationService.evaluationCases().size()
                    && results.stream().allMatch(result -> "SUCCEEDED".equals(result.status()));
            runStore.saveProgress(runId,
                    allSucceeded
                            ? ResumeEvidenceAuditEvaluationRun.Status.SUCCEEDED
                            : ResumeEvidenceAuditEvaluationRun.Status.PARTIAL_FAILED,
                    results,
                    allSucceeded ? null : "CASE_EVALUATION_FAILED",
                    true);
        } catch (RuntimeException exception) {
            ResumeEvidenceAuditEvaluationRun current = runStore.find(runId).orElse(null);
            if (current != null) {
                runStore.saveProgress(runId,
                        ResumeEvidenceAuditEvaluationRun.Status.PARTIAL_FAILED,
                        current.caseResults(), exception.getClass().getSimpleName(), true);
            }
        } finally {
            scheduledRuns.remove(runId);
        }
    }

    private List<FixedResumeEvidenceAuditEvaluationService.CaseResult> orderedResults(
            Map<String, FixedResumeEvidenceAuditEvaluationService.CaseResult> resultsByCase) {
        Map<String, Integer> orderByCase = new HashMap<>();
        List<FixedResumeEvidenceAuditDataset.Case> cases = evaluationService.evaluationCases();
        for (int index = 0; index < cases.size(); index++) {
            orderByCase.put(cases.get(index).id(), index);
        }
        return resultsByCase.values().stream()
                .sorted(Comparator.comparingInt(result -> orderByCase.getOrDefault(
                        result.caseId(), Integer.MAX_VALUE)))
                .toList();
    }

    private boolean currentContract(ResumeEvidenceAuditEvaluationRun run) {
        return run.datasetVersion().equals(evaluationService.datasetVersion())
                && run.role().equals(evaluationService.roleName())
                && run.promptVersion().equals(evaluationService.promptVersion())
                && run.modelName().equals(evaluationService.modelName())
                && run.totalCaseCount() == evaluationService.evaluationCases().size();
    }

    private RunView toView(ResumeEvidenceAuditEvaluationRun run) {
        int succeededCount = (int) run.caseResults().stream()
                .filter(result -> "SUCCEEDED".equals(result.status()))
                .count();
        int failedCount = (int) run.caseResults().stream()
                .filter(result -> "FAILED".equals(result.status()))
                .count();
        FixedResumeEvidenceAuditEvaluationService.Report report = run.terminal()
                && run.caseResults().size() == run.totalCaseCount()
                ? evaluationService.report(
                        run.runId(), run.datasetVersion(), run.role(), run.promptVersion(),
                        run.modelName(), run.caseResults())
                : null;
        return new RunView(
                run.runId(), run.status(), run.datasetVersion(), run.role(), run.promptVersion(),
                run.modelName(), run.totalCaseCount(), succeededCount, failedCount,
                run.totalCaseCount() - run.caseResults().size(), run.errorType(),
                run.createdAt(), run.updatedAt(), run.completedAt(), run.caseResults(), report);
    }

    public record RunView(
            String runId,
            ResumeEvidenceAuditEvaluationRun.Status status,
            String datasetVersion,
            String role,
            String promptVersion,
            String modelName,
            int totalCaseCount,
            int succeededCaseCount,
            int failedCaseCount,
            int pendingCaseCount,
            String errorType,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            java.time.LocalDateTime completedAt,
            List<FixedResumeEvidenceAuditEvaluationService.CaseResult> cases,
            FixedResumeEvidenceAuditEvaluationService.Report report
    ) {
    }
}
