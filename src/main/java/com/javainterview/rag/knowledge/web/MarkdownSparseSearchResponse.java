package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;

public record MarkdownSparseSearchResponse(String query, int topK, String algorithm,
                                           int resultCount, List<MarkdownSearchResult> results) {

    public static MarkdownSparseSearchResponse from(String query, int topK,
                                                    List<MarkdownSearchResult> results) {
        return new MarkdownSparseSearchResponse(query, topK, "BM25", results.size(), results);
    }
}
