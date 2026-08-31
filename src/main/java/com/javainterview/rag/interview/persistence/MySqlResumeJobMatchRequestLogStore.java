package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLog;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogStore;

import java.util.List;
import java.util.Objects;

public class MySqlResumeJobMatchRequestLogStore implements ResumeJobMatchRequestLogStore {

    private final ResumeJobMatchRequestLogMapper mapper;

    public MySqlResumeJobMatchRequestLogStore(ResumeJobMatchRequestLogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "岗位匹配请求日志 Mapper 不能为空");
    }

    @Override
    public void record(ResumeJobMatchRequestLog log) {
        mapper.insert(ResumeJobMatchRequestLogEntity.from(log));
    }

    @Override
    public List<ResumeJobMatchRequestLog> findRecent(int limit) {
        return mapper.findRecent(limit).stream().map(entity -> new ResumeJobMatchRequestLog(
                entity.getId(), entity.getRequestId(), entity.getRoleName(), entity.getPromptVersion(),
                entity.getModelName(), ResumeJobMatchRequestLog.Status.valueOf(entity.getStatus()),
                entity.getPromptTokens(), entity.getCompletionTokens(), entity.getTotalTokens(),
                entity.getElapsedMillis(), entity.getErrorType(), entity.getCreatedAt())).toList();
    }
}
