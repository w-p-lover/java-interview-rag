package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentCaseResult;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentDecision;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentRun;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentStatus;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentStore;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentStrategy;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentSplit;

import java.util.List;
import java.util.Optional;

public final class MySqlRagRetrievalExperimentStore implements RagRetrievalExperimentStore {

    private static final TypeReference<List<RagRetrievalExperimentCaseResult>> CASE_RESULTS_TYPE =
            new TypeReference<>() { };

    private final RagRetrievalExperimentRunMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlRagRetrievalExperimentStore(
            RagRetrievalExperimentRunMapper mapper,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RagRetrievalExperimentRun createOrFind(RagRetrievalExperimentRun run) {
        RagRetrievalExperimentRunEntity entity = toEntity(run);
        mapper.insert(entity);
        RagRetrievalExperimentRunEntity existing = mapper.findByContract(run.contractHash());
        if (existing == null) {
            throw new IllegalStateException("检索实验运行创建失败");
        }
        return toRun(existing);
    }

    @Override
    public Optional<RagRetrievalExperimentRun> find(String runId) {
        RagRetrievalExperimentRunEntity entity = mapper.find(runId);
        return entity == null ? Optional.empty() : Optional.of(toRun(entity));
    }

    @Override
    public List<RagRetrievalExperimentRun> findRecent(int limit) {
        return mapper.findRecent(limit).stream().map(this::toRun).toList();
    }

    @Override
    public List<RagRetrievalExperimentRun> findRecoverable() {
        return mapper.findRecoverable().stream().map(this::toRun).toList();
    }

    @Override
    public void saveProgress(String runId,
                             RagRetrievalExperimentStatus status,
                             List<RagRetrievalExperimentCaseResult> caseResults,
                             RagRetrievalEvaluationReport report,
                             String errorType,
                             boolean completed) {
        try {
            int updated = mapper.saveProgress(runId, status.name(),
                    objectMapper.writeValueAsString(caseResults),
                    report == null ? null : objectMapper.writeValueAsString(report),
                    errorType, completed);
            if (updated == 0) {
                throw new IllegalStateException("检索实验运行不存在");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("检索实验结果序列化失败", exception);
        }
    }

    @Override
    public void saveDecision(String candidateRunId,
                             String baselineRunId,
                             RagRetrievalExperimentDecision decision,
                             String note) {
        if (mapper.saveDecision(candidateRunId, baselineRunId, decision.name(), note) == 0) {
            throw new IllegalStateException("候选实验运行不存在");
        }
    }

    private RagRetrievalExperimentRunEntity toEntity(RagRetrievalExperimentRun run) {
        try {
            RagRetrievalExperimentRunEntity entity = new RagRetrievalExperimentRunEntity();
            entity.setRunId(run.runId());
            entity.setName(run.name());
            entity.setStrategy(run.strategy().name());
            entity.setDatasetVersion(run.datasetVersion());
            entity.setDatasetSplit(run.datasetSplit().name());
            entity.setCorpusHash(run.corpusHash());
            entity.setIndexContract(run.indexContract());
            entity.setGitSha(run.gitSha());
            entity.setContractHash(run.contractHash());
            entity.setStatus(run.status().name());
            entity.setTotalCaseCount(run.totalCaseCount());
            entity.setCaseResultsJson(objectMapper.writeValueAsString(run.caseResults()));
            entity.setCreatedAt(run.createdAt());
            entity.setUpdatedAt(run.updatedAt());
            return entity;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("检索实验运行序列化失败", exception);
        }
    }

    private RagRetrievalExperimentRun toRun(RagRetrievalExperimentRunEntity entity) {
        try {
            return new RagRetrievalExperimentRun(
                    entity.getRunId(), entity.getName(),
                    RagRetrievalExperimentStrategy.valueOf(entity.getStrategy()),
                    entity.getDatasetVersion(),
                    RagRetrievalExperimentSplit.valueOf(entity.getDatasetSplit()),
                    entity.getCorpusHash(), entity.getIndexContract(), entity.getGitSha(),
                    entity.getContractHash(),
                    RagRetrievalExperimentStatus.valueOf(entity.getStatus()),
                    entity.getTotalCaseCount(),
                    objectMapper.readValue(entity.getCaseResultsJson(), CASE_RESULTS_TYPE),
                    entity.getReportJson() == null ? null
                            : objectMapper.readValue(entity.getReportJson(),
                                    RagRetrievalEvaluationReport.class),
                    entity.getErrorType(), entity.getBaselineRunId(),
                    entity.getDecision() == null ? null
                            : RagRetrievalExperimentDecision.valueOf(entity.getDecision()),
                    entity.getDecisionNote(), entity.getCreatedAt(), entity.getUpdatedAt(),
                    entity.getCompletedAt(), entity.getDecidedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("检索实验运行反序列化失败", exception);
        }
    }
}
