package com.javainterview.rag.interview.evaluation;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class AnswerExperimentService {
    private final FixedInterviewAnswerEvaluationDataset dataset;
    private final InterviewAnswerEvaluator evaluator;
    private final AnswerExperimentContractProvider contracts;
    private final AnswerExperimentStore store;
    private final Executor executor;
    private final Set<String> scheduled = ConcurrentHashMap.newKeySet();
    public AnswerExperimentService(FixedInterviewAnswerEvaluationDataset dataset, InterviewAnswerEvaluator evaluator,
                                   AnswerExperimentStore store, Executor executor) {
        this.dataset = dataset; this.evaluator = evaluator; this.contracts = new AnswerExperimentContractProvider(dataset, evaluator);
        this.store = store; this.executor = executor;
    }
    @PostConstruct public void recover() { store.findRecoverable().forEach(run -> { if (current(run)) schedule(run.runId());
        else store.saveProgress(run.runId(), AnswerExperimentStatus.PARTIAL_FAILED, run.caseResults(), null, "RUN_CONTRACT_CHANGED", true); }); }
    public RunView start(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) throw new IllegalArgumentException("实验名称不能为空且不能超过 100 个字符");
        AnswerExperimentContractProvider.Contract c = contracts.current(); LocalDateTime now = LocalDateTime.now();
        AnswerExperimentRun run = new AnswerExperimentRun(UUID.randomUUID().toString(), name.strip(), c.datasetVersion(), c.promptVersion(), c.rubricVersion(), c.modelName(), c.contractHash(), AnswerExperimentStatus.QUEUED, c.totalCaseCount(), List.of(), null, null, null, null, null, now, now, null, null);
        AnswerExperimentRun actual = store.createOrFind(run); if (!actual.terminal()) schedule(actual.runId()); return view(actual);
    }
    public List<RunView> recent(int limit) { if (limit < 1 || limit > 50) throw new IllegalArgumentException("实验运行数量必须在 1 到 50 之间"); return store.findRecent(limit).stream().map(this::view).toList(); }
    public RunView find(String id) { return view(require(id)); }
    public RunView resume(String id) { AnswerExperimentRun run = require(id); if (!current(run)) throw new IllegalStateException("回答实验契约已变化，不能续跑"); if (run.status() != AnswerExperimentStatus.SUCCEEDED) schedule(id); return find(id); }
    public Comparison compare(String baselineId, String candidateId) {
        AnswerExperimentRun b = completed(baselineId), c = completed(candidateId);
        if (b.runId().equals(c.runId()) || !b.datasetVersion().equals(c.datasetVersion())) throw new IllegalStateException("基线和候选必须使用同一回答数据集且不能是同一次运行");
        AnswerExperimentReport br = b.report(), cr = c.report();
        List<MetricDelta> metrics = List.of(delta("passRate", "评价通过率", br.passRate(), cr.passRate(), true), delta("groundedAccuracy", "知识依据判断准确率", br.groundedAccuracy(), cr.groundedAccuracy(), true), delta("citationCompleteness", "引用完整率", br.citationCompleteness(), cr.citationCompleteness(), true), delta("averageOverallScore", "平均总分", br.averageOverallScore(), cr.averageOverallScore(), true), delta("latencyP95Ms", "评价 P95 延迟", br.latencyP95Ms(), cr.latencyP95Ms(), false));
        List<String> failures = new ArrayList<>(); if (cr.groundedAccuracy() < br.groundedAccuracy()) failures.add("知识依据判断准确率下降"); if (cr.citationCompleteness() < br.citationCompleteness()) failures.add("引用完整率下降");
        return new Comparison(view(b), view(c), metrics, failures.isEmpty(), failures);
    }
    public RunView decide(String candidateId, String baselineId, AnswerExperimentDecision decision, String note) { compare(baselineId, candidateId); if (note != null && note.strip().length() > 500) throw new IllegalArgumentException("结论说明不能超过 500 个字符"); store.saveDecision(candidateId, baselineId, decision, note == null ? "" : note.strip()); return find(candidateId); }
    private void schedule(String id) { if (!scheduled.add(id)) return; try { executor.execute(() -> execute(id)); } catch (RuntimeException e) { scheduled.remove(id); throw e; } }
    private void execute(String id) { try { AnswerExperimentRun run = require(id); if (!current(run)) { store.saveProgress(id, AnswerExperimentStatus.PARTIAL_FAILED, run.caseResults(), null, "RUN_CONTRACT_CHANGED", true); return; } Map<String, AnswerExperimentCaseResult> results = new HashMap<>(); run.caseResults().forEach(r -> results.put(r.caseId(), r)); store.saveProgress(id, AnswerExperimentStatus.RUNNING, ordered(results), null, null, false);
            for (FixedInterviewAnswerEvaluationDataset.Case c : dataset.findAll()) { if (results.containsKey(c.id()) && results.get(c.id()).status() == AnswerExperimentCaseResult.Status.SUCCEEDED) continue; long started = System.nanoTime(); try { InterviewAnswerEvaluation e = evaluator.evaluateBaseline(c.profile(), c.question(), c.answer(), id, c.id()).evaluation(); boolean passed = !c.expectedRejected() && e.overallScore() >= c.expectedMinOverallScore() && e.overallScore() <= c.expectedMaxOverallScore() && e.knowledgeGrounded() == c.expectedKnowledgeGrounded(); results.put(c.id(), new AnswerExperimentCaseResult(c.id(), AnswerExperimentCaseResult.Status.SUCCEEDED, passed, c.expectedKnowledgeGrounded(), e.knowledgeGrounded(), e.overallScore(), e.answerEvidence().size(), e.knowledgeEvidence().size(), elapsed(started), null)); store.saveProgress(id, AnswerExperimentStatus.RUNNING, ordered(results), null, null, false); } catch (RuntimeException e) { if (c.expectedRejected()) { results.put(c.id(), new AnswerExperimentCaseResult(c.id(), AnswerExperimentCaseResult.Status.SUCCEEDED, true, c.expectedKnowledgeGrounded(), null, null, 0, 0, elapsed(started), e.getClass().getSimpleName())); store.saveProgress(id, AnswerExperimentStatus.RUNNING, ordered(results), null, null, false); } else { results.put(c.id(), new AnswerExperimentCaseResult(c.id(), AnswerExperimentCaseResult.Status.FAILED, false, c.expectedKnowledgeGrounded(), null, null, null, null, elapsed(started), e.getClass().getSimpleName())); store.saveProgress(id, AnswerExperimentStatus.PARTIAL_FAILED, ordered(results), null, e.getClass().getSimpleName(), true); return; } } }
            List<AnswerExperimentCaseResult> ordered = ordered(results); store.saveProgress(id, AnswerExperimentStatus.SUCCEEDED, ordered, AnswerExperimentReport.from(ordered), null, true);
        } catch (RuntimeException e) { store.find(id).ifPresent(run -> store.saveProgress(id, AnswerExperimentStatus.PARTIAL_FAILED, run.caseResults(), null, e.getClass().getSimpleName(), true)); } finally { scheduled.remove(id); } }
    private long elapsed(long started) { return Math.max(0, (System.nanoTime() - started) / 1_000_000); }
    private List<AnswerExperimentCaseResult> ordered(Map<String, AnswerExperimentCaseResult> values) { Map<String,Integer> order = new HashMap<>(); for (int i=0;i<dataset.findAll().size();i++) order.put(dataset.findAll().get(i).id(), i); return values.values().stream().sorted(Comparator.comparingInt(r -> order.getOrDefault(r.caseId(), Integer.MAX_VALUE))).toList(); }
    private boolean current(AnswerExperimentRun run) { return run.contractHash().equals(contracts.current().contractHash()) && run.totalCaseCount() == contracts.current().totalCaseCount(); }
    private AnswerExperimentRun require(String id) { return store.find(id).orElseThrow(() -> new NoSuchElementException("回答实验不存在")); }
    private AnswerExperimentRun completed(String id) { AnswerExperimentRun r = require(id); if (r.status() != AnswerExperimentStatus.SUCCEEDED || r.report() == null) throw new IllegalStateException("只能比较已成功完成的回答实验"); return r; }
    private RunView view(AnswerExperimentRun run) { int ok=(int)run.caseResults().stream().filter(r->r.status()==AnswerExperimentCaseResult.Status.SUCCEEDED).count(); int failed=run.caseResults().size()-ok; return new RunView(run,ok,failed,Math.max(0,run.totalCaseCount()-ok-failed)); }
    private MetricDelta delta(String code,String label,double b,double c,boolean higher){return new MetricDelta(code,label,b,c,c-b,higher);}
    public record RunView(AnswerExperimentRun run,int succeededCaseCount,int failedCaseCount,int pendingCaseCount){}
    public record MetricDelta(String code,String label,double baseline,double candidate,double delta,boolean higherIsBetter){}
    public record Comparison(RunView baseline,RunView candidate,List<MetricDelta> metrics,boolean safeguardsPassed,List<String> safeguardFailures){ public Comparison{metrics=List.copyOf(metrics);safeguardFailures=List.copyOf(safeguardFailures);}}
}
