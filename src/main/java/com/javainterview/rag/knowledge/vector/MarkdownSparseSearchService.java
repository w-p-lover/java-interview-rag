package com.javainterview.rag.knowledge.vector;

import java.util.List;
import java.util.Objects;

public final class MarkdownSparseSearchService {

    private final LuceneMarkdownChunkIndex index;
    private final SparseQueryEvidencePolicy evidencePolicy;

    public MarkdownSparseSearchService(LuceneMarkdownChunkIndex index) {
        this.index = Objects.requireNonNull(index, "BM25 索引不能为空");
        this.evidencePolicy = new SparseQueryEvidencePolicy();
    }

    public List<MarkdownSearchResult> search(String query, int topK) {
        List<MarkdownSearchResult> results = index.search(query, topK);
        return evidencePolicy.hasEnoughEvidence(query, results) ? results : List.of();
    }
}
