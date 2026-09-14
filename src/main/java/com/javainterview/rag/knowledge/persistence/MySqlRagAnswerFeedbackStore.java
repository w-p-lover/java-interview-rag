package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MySqlRagAnswerFeedbackStore implements RagAnswerFeedbackStore {
    private final RagAnswerFeedbackMapper mapper;

    public MySqlRagAnswerFeedbackStore(RagAnswerFeedbackMapper mapper) { this.mapper = mapper; }

    @Override
    public void record(RagAnswerFeedback feedback) {
        try { mapper.insert(RagAnswerFeedbackEntity.from(feedback)); }
        catch (RuntimeException exception) { log.warn("写入 RAG 回答反馈失败，不影响当前页面", exception); }
    }
}
