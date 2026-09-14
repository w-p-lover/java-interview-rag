package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MySqlRagAnswerFeedbackStore implements RagAnswerFeedbackStore {
    private final RagAnswerFeedbackMapper mapper;

    public MySqlRagAnswerFeedbackStore(RagAnswerFeedbackMapper mapper) { this.mapper = mapper; }

    @Override
    public void record(RagAnswerFeedback feedback) {
        try { mapper.insert(RagAnswerFeedbackEntity.from(feedback)); }
        catch (RuntimeException exception) { log.warn("写入 RAG 回答反馈失败，不影响当前页面", exception); }
    }

    @Override
    public List<RagAnswerFeedback> findRecent(int limit, String feedback, String reviewStatus) {
        return mapper.findRecent(limit, feedback, reviewStatus).stream().map(this::toFeedback).toList();
    }

    @Override
    public Optional<RagAnswerFeedback> findById(long id) {
        RagAnswerFeedbackEntity entity = mapper.selectById(id);
        return entity == null ? Optional.empty() : Optional.of(toFeedback(entity));
    }

    @Override
    public RagAnswerFeedback review(long id, String reviewStatus, String reviewNote, String failureLayer) {
        if (!"EVALUATION".equals(reviewStatus) && !"IGNORED".equals(reviewStatus)) {
            throw new IllegalArgumentException("反馈审阅状态不合法");
        }
        if (mapper.review(id, reviewStatus, reviewNote, failureLayer) == 0) {
            throw new IllegalArgumentException("反馈不存在");
        }
        RagAnswerFeedbackEntity entity = mapper.selectById(id);
        return entity == null ? null : toFeedback(entity);
    }

    private RagAnswerFeedback toFeedback(RagAnswerFeedbackEntity entity) {
        return new RagAnswerFeedback(entity.getId(), entity.getQuestionHash(), Boolean.TRUE.equals(entity.getGrounded()),
                entity.getRetrievalStrategy(), entity.getFeedback(), entity.getReviewStatus(), entity.getReviewNote(), entity.getFailureLayer(),
                entity.getCreatedAt(), entity.getReviewedAt());
    }
}
