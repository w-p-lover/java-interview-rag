package com.javainterview.rag.interview.matching;

import java.util.List;

public interface ResumeJobMatchRequestLogStore {

    void record(ResumeJobMatchRequestLog log);

    List<ResumeJobMatchRequestLog> findRecent(int limit);
}
