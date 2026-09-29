package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class ScoreStabilityServiceTest {
    @Test
    void repeatsScoresAndCalculatesStabilityReport() {
        InMemoryStore store = new InMemoryStore();
        ScoreStabilityService service = new ScoreStabilityService(new FixedInterviewAnswerEvaluationDataset(), evaluator(), store, Runnable::run);
        ScoreStabilityService.RunView view = service.start("稳定性测试");
        view = service.find(view.run().runId());
        assertEquals(ScoreStabilityStatus.SUCCEEDED, view.run().status());
        assertEquals(5, view.succeededCaseCount());
        assertEquals(8, view.run().report().chatCallCount());
        assertEquals(1.0, view.run().report().stableCaseRate());
        assertEquals(0.0, view.run().report().averageScoreRange());
    }

    @Test
    void reusesContractAndBlocksDifferentDatasetComparison() {
        InMemoryStore store = new InMemoryStore();
        ScoreStabilityService service = new ScoreStabilityService(new FixedInterviewAnswerEvaluationDataset(), evaluator(), store, Runnable::run);
        ScoreStabilityService.RunView first = service.start("first");
        ScoreStabilityService.RunView repeated = service.start("second");
        assertEquals(first.run().runId(), repeated.run().runId());
        assertThrows(IllegalStateException.class, () -> service.compare(first.run().runId(), first.run().runId()));
    }

    private InterviewAnswerEvaluator evaluator() {
        return new InterviewAnswerEvaluator() {
            public InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile, InterviewQuestion question, String answer) {
                return evaluateBaseline(profile, question, answer, "test", "test");
            }
            public InterviewAnswerAssessment evaluateBaseline(InterviewPreparationProfile profile, InterviewQuestion question,
                                                               String answer, String runId, String caseId) {
                if (answer.isBlank()) throw new IllegalArgumentException("回答不能为空");
                boolean grounded = !answer.contains("线程泄漏");
                int score = answer.contains("页面开发") ? 20 : answer.contains("删除旧版本") ? 60 : 80;
                return new InterviewAnswerAssessment(new InterviewAnswerEvaluation(score, score, score, score,
                        List.of("结构清晰"), List.of("补充边界"), "继续说明失败恢复。", grounded,
                        List.of("固定回答"), grounded ? List.of(new InterviewKnowledgeEvidence("source", "v1", 0, List.of("RAG"), 1.0, "证据")) : List.of()), null);
            }
            public String promptVersion() { return "prompt-test"; }
            public String rubricVersion() { return "rubric-test"; }
            public String modelName() { return "model-test"; }
        };
    }

    private static final class InMemoryStore implements ScoreStabilityStore {
        private final Map<String, ScoreStabilityRun> runs = new LinkedHashMap<>();
        public ScoreStabilityRun createOrFind(ScoreStabilityRun run) { return runs.computeIfAbsent(run.contractHash(), ignored -> run); }
        public Optional<ScoreStabilityRun> find(String runId) { return runs.values().stream().filter(run -> run.runId().equals(runId)).findFirst(); }
        public List<ScoreStabilityRun> findRecent(int limit) { return runs.values().stream().limit(limit).toList(); }
        public List<ScoreStabilityRun> findRecoverable() { return runs.values().stream().filter(run -> !run.terminal()).toList(); }
        public void saveProgress(String id, ScoreStabilityStatus status, List<ScoreStabilityCaseResult> results, ScoreStabilityReport report, String error, boolean completed) {
            ScoreStabilityRun old = find(id).orElseThrow();
            ScoreStabilityRun updated = new ScoreStabilityRun(old.runId(), old.name(), old.datasetVersion(), old.promptVersion(), old.rubricVersion(), old.modelName(), old.contractHash(), status, old.totalCaseCount(), old.repeatCount(), results, report, error, old.baselineRunId(), old.decision(), old.decisionNote(), old.createdAt(), LocalDateTime.now(), completed ? LocalDateTime.now() : null, old.decidedAt());
            runs.put(old.contractHash(), updated);
        }
        public void saveDecision(String candidate, String baseline, AnswerExperimentDecision decision, String note) { }
    }
}
