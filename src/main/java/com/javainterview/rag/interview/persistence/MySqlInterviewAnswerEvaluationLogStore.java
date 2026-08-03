package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogStore;

import java.util.List;
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

    @Override
    public List<InterviewAnswerEvaluationLog> findRecent(
            InterviewAnswerEvaluationLog.Mode mode,
            String baselineRunId,
            String baselineCaseId,
            int limit) {
        return mapper.findRecent(mode.name(), baselineRunId, baselineCaseId, limit)
                .stream()
                .map(this::toLog)
                .toList();
    }

    private InterviewAnswerEvaluationLog toLog(InterviewAnswerEvaluationLogEntity entity) {
        return new InterviewAnswerEvaluationLog(
                entity.getId(),
                InterviewAnswerEvaluationLog.Mode.valueOf(entity.getEvaluationMode()),
                entity.getBaselineRunId(),
                entity.getBaselineCaseId(),
                entity.getPromptVersion(),
                entity.getRubricVersion(),
                entity.getModelName(),
                InterviewAnswerEvaluationLog.Status.valueOf(entity.getStatus()),
                entity.getKnowledgeGrounded(),
                entity.getTechnicalScore(),
                entity.getCompletenessScore(),
                entity.getCommunicationScore(),
                entity.getOverallScore(),
                entity.getElapsedMillis(),
                entity.getErrorType(),
                entity.getCreatedAt());
    }
}
