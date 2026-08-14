package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Objects;

public class ResumeEvidenceAuditRequestLogQueryService {

    private static final int MAX_LIMIT = 100;

    private final ResumeEvidenceAuditRequestLogStore logStore;

    public ResumeEvidenceAuditRequestLogQueryService(ResumeEvidenceAuditRequestLogStore logStore) {
        this.logStore = Objects.requireNonNull(logStore, "简历审计请求日志存储不能为空");
    }

    public List<ResumeEvidenceAuditRequestLog> findRecent(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("简历审计请求日志查询数量必须在 1 到 100 之间");
        }
        return logStore.findRecent(limit);
    }
}
