package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogStore;

import java.util.Objects;

public class MySqlInterviewAnswerEvaluationLogStore implements InterviewAnswerEvaluationLogStore {

    private final InterviewAnswerEvaluationLogMapper mapper;

    public MySqlInterviewAnswerEvaluationLogStore(InterviewAnswerEvaluationLogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "面试回答评价日志 Mapper 不能为空");
    }

    @Override
    public void record(InterviewAnswerEvaluationLog log) {
        mapper.insert(InterviewAnswerEvaluationLogEntity.from(log));
    }
}
