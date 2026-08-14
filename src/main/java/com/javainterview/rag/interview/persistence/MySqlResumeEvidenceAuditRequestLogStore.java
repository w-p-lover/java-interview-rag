package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogStore;

import java.util.List;
import java.util.Objects;

public class MySqlResumeEvidenceAuditRequestLogStore
        implements ResumeEvidenceAuditRequestLogStore {

    private final ResumeEvidenceAuditRequestLogMapper mapper;

    public MySqlResumeEvidenceAuditRequestLogStore(ResumeEvidenceAuditRequestLogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "简历审计请求日志 Mapper 不能为空");
    }

    @Override
    public void record(ResumeEvidenceAuditRequestLog log) {
        mapper.insert(ResumeEvidenceAuditRequestLogEntity.from(log));
    }

    @Override
    public List<ResumeEvidenceAuditRequestLog> findRecent(int limit) {
        return mapper.findRecent(limit).stream()
                .map(this::toLog)
                .toList();
    }

    private ResumeEvidenceAuditRequestLog toLog(ResumeEvidenceAuditRequestLogEntity entity) {
        return new ResumeEvidenceAuditRequestLog(
                entity.getId(),
                entity.getRequestId(),
                entity.getRoleName(),
                entity.getPromptVersion(),
                entity.getModelName(),
                ResumeEvidenceAuditRequestLog.Status.valueOf(entity.getStatus()),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getElapsedMillis(),
                entity.getErrorType(),
                entity.getCreatedAt());
    }
}
