package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;

import java.util.List;

public record MarkdownSparseSearchResponse(String query, int topK, String algorithm,
                                           String fieldWeightPolicy, int resultCount,
                                           List<MarkdownSearchResult> results) {

    public static MarkdownSparseSearchResponse from(String query, int topK,
                                                    List<MarkdownSearchResult> results) {
        return new MarkdownSparseSearchResponse(query, topK, "BM25-fielded-anchor-gate-v1",
                LuceneMarkdownChunkIndex.FIELD_WEIGHT_POLICY, results.size(), results);
    }
}
