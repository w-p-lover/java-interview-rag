package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRun;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRunStore;

import java.util.List;
import java.util.Optional;

public final class MySqlResumeEvidenceAuditEvaluationRunStore
        implements ResumeEvidenceAuditEvaluationRunStore {

    private static final TypeReference<List<FixedResumeEvidenceAuditEvaluationService.CaseResult>>
            CASE_RESULTS_TYPE = new TypeReference<>() {
            };

    private final ResumeEvidenceAuditEvaluationRunMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlResumeEvidenceAuditEvaluationRunStore(
            ResumeEvidenceAuditEvaluationRunMapper mapper,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResumeEvidenceAuditEvaluationRun createOrFind(
            String runId,
            String datasetVersion,
            String role,
            String promptVersion,
            String modelName,
            int totalCaseCount) {
        String contractKey = contractKey(datasetVersion, role, promptVersion, modelName);
        mapper.insert(runId, contractKey, datasetVersion, role, promptVersion, modelName,
                totalCaseCount);
        ResumeEvidenceAuditEvaluationRunEntity entity = mapper.findByContract(contractKey);
        if (entity == null) {
            throw new IllegalStateException("简历审计评估运行创建失败");
        }
        return toRun(entity);
    }

    @Override
    public Optional<ResumeEvidenceAuditEvaluationRun> find(String runId) {
        ResumeEvidenceAuditEvaluationRunEntity entity = mapper.find(runId);
        return entity == null ? Optional.empty() : Optional.of(toRun(entity));
    }

    @Override
    public List<ResumeEvidenceAuditEvaluationRun> findRecoverable() {
        return mapper.findRecoverable().stream().map(this::toRun).toList();
    }

    @Override
    public void saveProgress(
            String runId,
            ResumeEvidenceAuditEvaluationRun.Status status,
            List<FixedResumeEvidenceAuditEvaluationService.CaseResult> caseResults,
            String errorType,
            boolean completed) {
        try {
            int updated = mapper.saveProgress(runId, status.name(),
                    objectMapper.writeValueAsString(caseResults), errorType, completed);
            if (updated == 0) {
                throw new IllegalStateException("简历审计评估运行不存在");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历审计评估结果序列化失败", exception);
        }
    }

    private ResumeEvidenceAuditEvaluationRun toRun(
            ResumeEvidenceAuditEvaluationRunEntity entity) {
        try {
            return new ResumeEvidenceAuditEvaluationRun(
                    entity.getRunId(), entity.getDatasetVersion(), entity.getRoleName(),
                    entity.getPromptVersion(), entity.getModelName(),
                    ResumeEvidenceAuditEvaluationRun.Status.valueOf(entity.getStatus()),
                    entity.getTotalCaseCount(),
                    objectMapper.readValue(entity.getCaseResultsJson(), CASE_RESULTS_TYPE),
                    entity.getErrorType(), entity.getCreatedAt(), entity.getUpdatedAt(),
                    entity.getCompletedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历审计评估结果反序列化失败", exception);
        }
    }

    private String contractKey(String datasetVersion, String role,
                               String promptVersion, String modelName) {
        String contractKey = String.join("|", datasetVersion, role, promptVersion, modelName);
        if (contractKey.length() > 190) {
            throw new IllegalArgumentException("简历审计评估运行契约不能超过 190 个字符");
        }
        return contractKey;
    }
}
