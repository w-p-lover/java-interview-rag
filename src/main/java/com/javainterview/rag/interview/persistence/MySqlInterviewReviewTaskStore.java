package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import com.javainterview.rag.interview.review.InterviewReviewTaskStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MySqlInterviewReviewTaskStore implements InterviewReviewTaskStore {

    private static final TypeReference<List<String>> GAP_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<InterviewReviewKnowledgeSource>> SOURCE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final InterviewReviewTaskMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlInterviewReviewTaskStore(InterviewReviewTaskMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "复习任务 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public void createIfAbsent(InterviewReviewTask task) {
        try {
            mapper.insertIfAbsent(InterviewReviewTaskEntity.from(
                    task,
                    objectMapper.writeValueAsString(task.gaps()),
                    objectMapper.writeValueAsString(task.knowledgeSources())
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化复习任务失败", exception);
        }
    }

    @Override
    public List<InterviewReviewTask> findBySessionId(String sessionId) {
        return findBySessionId(sessionId, null, null, null);
    }

    @Override
    public List<InterviewReviewTask> findBySessionId(String sessionId,
                                                     InterviewReviewTaskStatus status,
                                                     LocalDate plannedDateFrom,
                                                     LocalDate plannedDateTo) {
        return mapper.findBySessionId(sessionId, status, plannedDateFrom, plannedDateTo)
                .stream().map(this::toTask).toList();
    }

    @Override
    public List<InterviewReviewTask> findBySessionIds(List<String> sessionIds) {
        return mapper.findBySessionIds(sessionIds).stream().map(this::toTask).toList();
    }

    @Override
    public Optional<InterviewReviewTask> findBySessionAndId(String sessionId, String taskId) {
        return Optional.ofNullable(mapper.findBySessionAndId(sessionId, taskId))
                .map(this::toTask);
    }

    @Override
    public Optional<InterviewReviewTask> complete(String sessionId, String taskId,
                                                  LocalDateTime completedAt) {
        mapper.complete(sessionId, taskId, completedAt);
        return Optional.ofNullable(mapper.findBySessionAndId(sessionId, taskId))
                .map(this::toTask);
    }

    @Override
    public Optional<InterviewReviewTask> schedule(String sessionId, String taskId,
                                                  LocalDate plannedReviewDate) {
        mapper.schedule(sessionId, taskId, plannedReviewDate);
        return Optional.ofNullable(mapper.findBySessionAndId(sessionId, taskId))
                .map(this::toTask);
    }

    private InterviewReviewTask toTask(InterviewReviewTaskEntity entity) {
        try {
            return new InterviewReviewTask(
                    entity.getId(),
                    entity.getSessionId(),
                    entity.getQuestionIndex(),
                    entity.getQuestion(),
                    entity.getObjective(),
                    objectMapper.readValue(entity.getGapsJson(), GAP_LIST_TYPE),
                    entity.getImprovementSuggestion(),
                    Boolean.TRUE.equals(entity.getKnowledgeGrounded()),
                    objectMapper.readValue(entity.getKnowledgeSourcesJson(), SOURCE_LIST_TYPE),
                    InterviewReviewTaskStatus.valueOf(entity.getStatus()),
                    entity.getPlannedReviewDate(),
                    entity.getCreatedAt(),
                    entity.getCompletedAt()
            );
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("复习任务数据损坏", exception);
        }
    }
}
