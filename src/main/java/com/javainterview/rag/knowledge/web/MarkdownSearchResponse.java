package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;

public record MarkdownSearchResponse(String query, int topK, double similarityThreshold,
                                     int resultCount, List<MarkdownSearchResult> results) {

    public static MarkdownSearchResponse from(String query, int topK, double similarityThreshold,
                                              List<MarkdownSearchResult> results) {
        return new MarkdownSearchResponse(query, topK, similarityThreshold, results.size(), results);
    }
}
