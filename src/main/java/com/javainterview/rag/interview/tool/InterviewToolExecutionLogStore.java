package com.javainterview.rag.interview.tool;

import java.util.List;

public interface InterviewToolExecutionLogStore {

    void record(InterviewToolExecutionLog log);

    List<InterviewToolExecutionLog> findRecent(String sessionId, int limit);
}
