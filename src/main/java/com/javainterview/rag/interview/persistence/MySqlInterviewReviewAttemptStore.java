package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.review.InterviewReviewAttempt;
import com.javainterview.rag.interview.review.InterviewReviewAttemptConflictException;
import com.javainterview.rag.interview.review.InterviewReviewAttemptStore;
import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Objects;

public class MySqlInterviewReviewAttemptStore implements InterviewReviewAttemptStore {

    private static final TypeReference<List<InterviewReviewKnowledgeSource>> SOURCE_LIST_TYPE =
            new TypeReference<>() { };

    private final InterviewReviewAttemptMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlInterviewReviewAttemptStore(InterviewReviewAttemptMapper mapper,
                                            ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "复习回答 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public void create(InterviewReviewAttempt attempt) {
        try {
            mapper.insertAttempt(InterviewReviewAttemptEntity.from(
                    attempt,
                    objectMapper.writeValueAsString(attempt.evaluation()),
                    objectMapper.writeValueAsString(attempt.knowledgeSources())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化复习回答失败", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new InterviewReviewAttemptConflictException("复习回答序号已被占用，请刷新后重试");
        }
    }

    @Override
    public List<InterviewReviewAttempt> findByReviewTaskId(String sessionId, String reviewTaskId) {
        return mapper.findByReviewTaskId(sessionId, reviewTaskId).stream()
                .map(this::toAttempt)
                .toList();
    }

    private InterviewReviewAttempt toAttempt(InterviewReviewAttemptEntity entity) {
        try {
            return new InterviewReviewAttempt(
                    entity.getId(), entity.getSessionId(), entity.getReviewTaskId(),
                    entity.getQuestionIndex(), entity.getAttemptNumber(), entity.getAnswer(),
                    objectMapper.readValue(entity.getEvaluationJson(), InterviewAnswerEvaluation.class),
                    Boolean.TRUE.equals(entity.getKnowledgeGrounded()),
                    objectMapper.readValue(entity.getKnowledgeSourcesJson(), SOURCE_LIST_TYPE),
                    entity.getPromptVersion(), entity.getRubricVersion(), entity.getModelName(),
                    entity.getOriginalOverallScore(), entity.getPreviousOverallScore(),
                    entity.getScoreDeltaFromOriginal(), entity.getScoreDeltaFromPrevious(),
                    entity.getCreatedAt());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("复习回答数据损坏", exception);
        }
    }
}
