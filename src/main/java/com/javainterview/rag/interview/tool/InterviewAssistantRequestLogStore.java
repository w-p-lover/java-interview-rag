package com.javainterview.rag.interview.tool;

import java.util.List;

public interface InterviewAssistantRequestLogStore {

    void record(InterviewAssistantRequestLog log);

    List<InterviewAssistantRequestLog> findRecent(int limit);
}
