package com.javainterview.rag.interview.matching;

import java.util.List;
import java.util.Objects;

public class ResumeJobMatchRequestLogQueryService {

    private static final int MAX_LIMIT = 100;
    private final ResumeJobMatchRequestLogStore logStore;

    public ResumeJobMatchRequestLogQueryService(ResumeJobMatchRequestLogStore logStore) {
        this.logStore = Objects.requireNonNull(logStore, "岗位匹配请求日志存储不能为空");
    }

    public List<ResumeJobMatchRequestLog> findRecent(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("岗位匹配请求日志查询数量必须在 1 到 100 之间");
        }
        return logStore.findRecent(limit);
    }
}
