package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.evaluation.*;
import java.util.*;

public final class MySqlScoreStabilityStore implements ScoreStabilityStore {
    private static final TypeReference<List<ScoreStabilityCaseResult>> CASES = new TypeReference<>() { };
    private final ScoreStabilityRunMapper mapper;
    private final ObjectMapper json;

    public MySqlScoreStabilityStore(ScoreStabilityRunMapper mapper, ObjectMapper json) {
        this.mapper = mapper; this.json = json;
    }
    public ScoreStabilityRun createOrFind(ScoreStabilityRun run) {
        mapper.insert(entity(run));
        ScoreStabilityRunEntity entity = mapper.findByContract(run.contractHash());
        if (entity == null) throw new IllegalStateException("评分稳定性实验创建失败");
        return run(entity);
    }
    public Optional<ScoreStabilityRun> find(String id) { ScoreStabilityRunEntity entity = mapper.find(id); return entity == null ? Optional.empty() : Optional.of(run(entity)); }
    public List<ScoreStabilityRun> findRecent(int limit) { return mapper.recent(limit).stream().map(this::run).toList(); }
    public List<ScoreStabilityRun> findRecoverable() { return mapper.recoverable().stream().map(this::run).toList(); }
    public void saveProgress(String id, ScoreStabilityStatus status, List<ScoreStabilityCaseResult> results,
                             ScoreStabilityReport report, String errorType, boolean completed) {
        try { mapper.progress(id, status.name(), json.writeValueAsString(results),
                report == null ? null : json.writeValueAsString(report), errorType, completed); }
        catch (Exception exception) { throw new IllegalStateException("评分稳定性实验结果保存失败", exception); }
    }
    public void saveDecision(String candidate, String baseline, AnswerExperimentDecision decision, String note) {
        if (mapper.decision(candidate, baseline, decision.name(), note) == 0) throw new IllegalStateException("评分稳定性实验不存在");
    }
    private ScoreStabilityRunEntity entity(ScoreStabilityRun run) {
        try {
            ScoreStabilityRunEntity entity = new ScoreStabilityRunEntity();
            entity.setRunId(run.runId()); entity.setName(run.name()); entity.setDatasetVersion(run.datasetVersion());
            entity.setPromptVersion(run.promptVersion()); entity.setRubricVersion(run.rubricVersion());
            entity.setModelName(run.modelName()); entity.setContractHash(run.contractHash());
            entity.setStatus(run.status().name()); entity.setTotalCaseCount(run.totalCaseCount());
            entity.setRepeatCount(run.repeatCount()); entity.setCaseResultsJson(json.writeValueAsString(run.caseResults()));
            entity.setCreatedAt(run.createdAt()); entity.setUpdatedAt(run.updatedAt()); return entity;
        } catch (Exception exception) { throw new IllegalStateException("评分稳定性实验序列化失败", exception); }
    }
    private ScoreStabilityRun run(ScoreStabilityRunEntity entity) {
        try {
            return new ScoreStabilityRun(entity.getRunId(), entity.getName(), entity.getDatasetVersion(),
                    entity.getPromptVersion(), entity.getRubricVersion(), entity.getModelName(), entity.getContractHash(),
                    ScoreStabilityStatus.valueOf(entity.getStatus()), entity.getTotalCaseCount(), entity.getRepeatCount(),
                    json.readValue(entity.getCaseResultsJson(), CASES),
                    entity.getReportJson() == null ? null : json.readValue(entity.getReportJson(), ScoreStabilityReport.class),
                    entity.getErrorType(), entity.getBaselineRunId(),
                    entity.getDecision() == null ? null : AnswerExperimentDecision.valueOf(entity.getDecision()),
                    entity.getDecisionNote(), entity.getCreatedAt(), entity.getUpdatedAt(),
                    entity.getCompletedAt(), entity.getDecidedAt());
        } catch (Exception exception) { throw new IllegalStateException("评分稳定性实验反序列化失败", exception); }
    }
}
