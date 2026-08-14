package com.javainterview.rag.interview.resume;

import java.util.List;

public interface ResumeEvidenceAuditRequestLogStore {

    void record(ResumeEvidenceAuditRequestLog log);

    List<ResumeEvidenceAuditRequestLog> findRecent(int limit);
}
