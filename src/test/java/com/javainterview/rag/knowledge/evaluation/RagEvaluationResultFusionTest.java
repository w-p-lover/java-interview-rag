package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagEvaluationResultFusionTest {

    private final RagEvaluationResultFusion fusion = new RagEvaluationResultFusion();

    @Test
    void fusesByRankAndDeduplicatesChunks() {
        MarkdownSearchResult originalFirst = result("concurrent", "java/concurrent-collections.md", 0, 0.72);
        MarkdownSearchResult shared = result("shared", "java/collections.md", 0, 0.61);
        MarkdownSearchResult expanded = result("expanded", "java/collections.md", 2, 0.77);

        List<MarkdownSearchResult> results = fusion.fuse(List.of(
                List.of(originalFirst, shared),
                List.of(expanded, shared)
        ), 3);

        assertEquals(List.of(shared, originalFirst, expanded), results);
    }

    @Test
    void keepsFirstSeenOrderForEqualFusionScoresAndHonorsTopK() {
        MarkdownSearchResult originalFirst = result("first", "java/collections.md", 0, 0.80);
        MarkdownSearchResult expandedFirst = result("second", "java/concurrent-collections.md", 0, 0.90);

        List<MarkdownSearchResult> results = fusion.fuse(List.of(
                List.of(originalFirst),
                List.of(expandedFirst)
        ), 1);

        assertEquals(List.of(originalFirst), results);
    }

    private MarkdownSearchResult result(String content, String source, int chunkOrder, double similarity) {
        return new MarkdownSearchResult(content, source, "version-1", chunkOrder,
                List.of("标题", content), similarity);
    }
}
