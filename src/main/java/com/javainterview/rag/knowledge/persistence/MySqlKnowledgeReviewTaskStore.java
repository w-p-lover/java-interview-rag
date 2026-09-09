package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.review.*;
import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class MySqlKnowledgeReviewTaskStore implements KnowledgeReviewTaskStore {
    private static final TypeReference<List<Integer>> SOURCE_TYPE = new TypeReference<>() {};
    private final KnowledgeReviewTaskMapper mapper;
    private final ObjectMapper objectMapper;
    public MySqlKnowledgeReviewTaskStore(KnowledgeReviewTaskMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper); this.objectMapper = Objects.requireNonNull(objectMapper);
    }
    public KnowledgeReviewTask createIfAbsent(KnowledgeReviewTask task) {
        try { mapper.insertIfAbsent(KnowledgeReviewTaskEntity.from(task, objectMapper.writeValueAsString(task.sourceNumbers()))); }
        catch (JsonProcessingException e) { throw new IllegalStateException("知识复习任务来源序列化失败", e); }
        return findRequired(task.id());
    }
    public List<KnowledgeReviewTask> find(KnowledgeReviewTaskStatus status, LocalDate from, LocalDate to) {
        return mapper.find(status, from, to).stream().map(this::toTask).toList();
    }
    public Optional<KnowledgeReviewTask> complete(String taskId, LocalDateTime at) { mapper.complete(taskId, at); return Optional.ofNullable(mapper.findById(taskId)).map(this::toTask); }
    public Optional<KnowledgeReviewTask> schedule(String taskId, LocalDate date) { mapper.schedule(taskId, date); return Optional.ofNullable(mapper.findById(taskId)).map(this::toTask); }
    private KnowledgeReviewTask findRequired(String id) { return Optional.ofNullable(mapper.findById(id)).map(this::toTask).orElseThrow(); }
    private KnowledgeReviewTask toTask(KnowledgeReviewTaskEntity e) {
        try { return new KnowledgeReviewTask(e.getId(), e.getQuestion(), e.getClaim(), RagAnswerClaimEvidenceStatus.valueOf(e.getEvidenceStatus()), objectMapper.readValue(e.getSourceNumbersJson(), SOURCE_TYPE), KnowledgeReviewTaskStatus.valueOf(e.getStatus()), e.getPlannedReviewDate(), e.getCreatedAt(), e.getCompletedAt()); }
        catch (JsonProcessingException | IllegalArgumentException ex) { throw new IllegalStateException("知识复习任务数据损坏", ex); }
    }
}
