package com.javainterview.rag.knowledge.web;

public record RagAnswerFeedbackRequest(String question, boolean grounded, String retrievalStrategy, String feedback) { }
