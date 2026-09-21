package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.resume.ResumeAnalysisResult;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultStore;
import com.javainterview.rag.interview.resume.ResumeAnalysisType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MySqlResumeAnalysisResultStore implements ResumeAnalysisResultStore {

    private final ResumeAnalysisResultMapper mapper;

    public MySqlResumeAnalysisResultStore(ResumeAnalysisResultMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "简历分析结果 Mapper 不能为空");
    }

    @Override
    public ResumeAnalysisResult saveSuccessful(ResumeAnalysisResult result) {
        mapper.upsert(entity(result));
        return result;
    }

    @Override
    public Optional<ResumeAnalysisResult> findLatest(
            String resumeVersionId, ResumeAnalysisType analysisType) {
        return Optional.ofNullable(mapper.findLatest(resumeVersionId, analysisType.name()))
                .map(this::result);
    }

    @Override
    public List<ResumeAnalysisResult> findAll(String resumeVersionId) {
        return mapper.findAll(resumeVersionId).stream().map(this::result).toList();
    }

    private ResumeAnalysisResultEntity entity(ResumeAnalysisResult result) {
        return new ResumeAnalysisResultEntity(
                result.id(), result.resumeVersionId(), result.analysisType().name(),
                result.inputFingerprint(), result.contractVersion(), result.modelName(),
                result.resultJson(), result.createdAt(), result.updatedAt());
    }

    private ResumeAnalysisResult result(ResumeAnalysisResultEntity entity) {
        return new ResumeAnalysisResult(
                entity.getId(), entity.getResumeVersionId(),
                ResumeAnalysisType.valueOf(entity.getAnalysisType()), entity.getInputFingerprint(),
                entity.getContractVersion(), entity.getModelName(), entity.getResultJson(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
