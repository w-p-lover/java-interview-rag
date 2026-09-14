package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.evaluation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MySqlRagEvaluationCandidateStore implements RagEvaluationCandidateStore {
    private static final TypeReference<List<String>> LIST = new TypeReference<>() { };
    private final RagEvaluationCandidateMapper mapper;
    private final ObjectMapper objectMapper;
    @Override public RagEvaluationCandidate createIfAbsent(RagEvaluationCandidate candidate) {
        try { mapper.insertIfAbsent(RagEvaluationCandidateEntity.from(candidate, objectMapper.writeValueAsString(candidate.expectedSources()), objectMapper.writeValueAsString(candidate.expectedAnswerPoints()))); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("评估候选序列化失败", exception); }
        return findById(candidate.id()).orElseThrow();
    }
    @Override public List<RagEvaluationCandidate> findRecent(int limit, RagEvaluationCandidateStatus status) { return mapper.findRecent(limit, status == null ? null : status.name()).stream().map(this::toCandidate).toList(); }
    @Override public Optional<RagEvaluationCandidate> findById(String id) { return Optional.ofNullable(mapper.selectById(id)).map(this::toCandidate); }
    @Override public RagEvaluationCandidate review(String id, RagEvaluationCandidateStatus status) { if (mapper.review(id, status.name()) == 0) throw new IllegalArgumentException("评估候选不存在"); return findById(id).orElseThrow(); }
    private RagEvaluationCandidate toCandidate(RagEvaluationCandidateEntity entity) {
        try { return new RagEvaluationCandidate(entity.getId(), entity.getFeedbackId(), entity.getQuestion(), objectMapper.readValue(entity.getExpectedSourcesJson(), LIST), objectMapper.readValue(entity.getExpectedAnswerPointsJson(), LIST), RagEvaluationCandidateStatus.valueOf(entity.getStatus()), entity.getCreatedAt(), entity.getReviewedAt()); }
        catch (JsonProcessingException | IllegalArgumentException exception) { throw new IllegalStateException("评估候选数据损坏", exception); }
    }
}
