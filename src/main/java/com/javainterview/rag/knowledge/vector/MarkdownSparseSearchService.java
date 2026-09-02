package com.javainterview.rag.knowledge.vector;

import java.util.List;
import java.util.Objects;

public final class MarkdownSparseSearchService {

    private final LuceneMarkdownChunkIndex index;

    public MarkdownSparseSearchService(LuceneMarkdownChunkIndex index) {
        this.index = Objects.requireNonNull(index, "BM25 索引不能为空");
    }

    public List<MarkdownSearchResult> search(String query, int topK) {
        return index.search(query, topK);
    }
}
