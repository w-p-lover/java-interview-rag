package com.javainterview.rag.knowledge.vector;

public final class KnowledgeRetrievalUnavailableException extends RuntimeException {

    public KnowledgeRetrievalUnavailableException(String message) {
        super(message);
    }

    public KnowledgeRetrievalUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
