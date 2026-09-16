package com.javainterview.rag.knowledge.vector;

import java.util.List;
import java.util.Objects;

public final class MarkdownSparseSearchService {

    private final LuceneMarkdownChunkIndex index;
    private final SparseQueryEvidencePolicy evidencePolicy;
    private final KnowledgeRetrievalDeadline deadline;

    public MarkdownSparseSearchService(LuceneMarkdownChunkIndex index) {
        this(index, null);
    }

    public MarkdownSparseSearchService(LuceneMarkdownChunkIndex index,
                                       KnowledgeRetrievalDeadline deadline) {
        this.index = Objects.requireNonNull(index, "BM25 索引不能为空");
        this.evidencePolicy = new SparseQueryEvidencePolicy();
        this.deadline = deadline;
    }

    public List<MarkdownSearchResult> search(String query, int topK) {
        List<MarkdownSearchResult> results = deadline == null
                ? index.search(query, topK)
                : deadline.execute("Sparse 稀疏检索", () -> index.search(query, topK));
        return evidencePolicy.hasEnoughEvidence(query, results) ? results : List.of();
    }
}
