package com.javainterview.rag.interview.evaluation;

import java.util.List;

public interface InterviewAnswerEvaluationLogStore {

    void record(InterviewAnswerEvaluationLog log);

    List<InterviewAnswerEvaluationLog> findRecent(
            InterviewAnswerEvaluationLog.Mode mode,
            String baselineRunId,
            String baselineCaseId,
            int limit);
}
