package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.rag.RagRetrievalLog;

import java.util.List;

public record RagAnswerFeedbackDiagnosis(RagAnswerFeedback feedback, List<RagRetrievalLog> retrievalLogs) {
    public RagAnswerFeedbackDiagnosis { retrievalLogs = List.copyOf(retrievalLogs); }
}
