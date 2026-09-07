package com.javainterview.rag.knowledge.rag;

public enum RagRetrievalLogStatus {
    ANSWERED,
    PARTIAL_ANSWER,
    INSUFFICIENT,
    CITATION_INVALID,
    EVIDENCE_INSUFFICIENT,
    EVIDENCE_CONFLICTING,
    FAILED,
    CANCELLED
}
