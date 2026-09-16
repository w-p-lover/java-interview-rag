package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalTimeoutException;
import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        MarkdownSearchController.class,
        MarkdownSparseSearchController.class,
        MarkdownHybridSearchController.class
})
public class KnowledgeRetrievalExceptionHandler {

    @ExceptionHandler(KnowledgeRetrievalTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Map<String, String> handleTimeout(KnowledgeRetrievalTimeoutException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(KnowledgeRetrievalUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleUnavailable(KnowledgeRetrievalUnavailableException exception) {
        return Map.of("message", exception.getMessage());
    }
}
