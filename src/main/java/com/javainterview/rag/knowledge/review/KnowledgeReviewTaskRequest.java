package com.javainterview.rag.knowledge.review;

import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;

import java.util.List;

public record KnowledgeReviewTaskRequest(String question, String claim,
                                         RagAnswerClaimEvidenceStatus evidenceStatus,
                                         List<Integer> sourceNumbers) {
}
