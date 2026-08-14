package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RagEvaluationResultFusion {

    private static final int RRF_RANK_CONSTANT = 60;

    public List<MarkdownSearchResult> fuse(List<List<MarkdownSearchResult>> rankedResults, int topK) {
        Objects.requireNonNull(rankedResults, "待融合检索结果不能为空");
        if (topK < 1) {
            throw new IllegalArgumentException("topK 必须大于 0");
        }

        Map<ChunkIdentity, FusedResult> fused = new LinkedHashMap<>();
        int firstSeenOrder = 0;
        for (List<MarkdownSearchResult> results : rankedResults) {
            Objects.requireNonNull(results, "单次检索结果不能为空");
            for (int index = 0; index < results.size(); index++) {
                MarkdownSearchResult result = Objects.requireNonNull(results.get(index), "检索结果不能为空");
                ChunkIdentity identity = ChunkIdentity.from(result);
                FusedResult current = fused.get(identity);
                double score = 1.0 / (RRF_RANK_CONSTANT + index + 1);
                if (current == null) {
                    fused.put(identity, new FusedResult(result, score, firstSeenOrder++));
                } else {
                    current.add(result, score);
                }
            }
        }

        return fused.values().stream()
                .sorted(Comparator.comparingDouble(FusedResult::score).reversed()
                        .thenComparingInt(FusedResult::firstSeenOrder))
                .limit(topK)
                .map(FusedResult::result)
                .toList();
    }

    private record ChunkIdentity(String source, String documentVersion, int chunkOrder) {

        private static ChunkIdentity from(MarkdownSearchResult result) {
            return new ChunkIdentity(result.source(), result.documentVersion(), result.chunkOrder());
        }
    }

    private static final class FusedResult {

        private MarkdownSearchResult result;
        private double score;
        private final int firstSeenOrder;

        private FusedResult(MarkdownSearchResult result, double score, int firstSeenOrder) {
            this.result = result;
            this.score = score;
            this.firstSeenOrder = firstSeenOrder;
        }

        private void add(MarkdownSearchResult candidate, double addedScore) {
            score += addedScore;
            if (similarity(candidate) > similarity(result)) {
                result = candidate;
            }
        }

        private double similarity(MarkdownSearchResult candidate) {
            return candidate.similarity() == null ? Double.NEGATIVE_INFINITY : candidate.similarity();
        }

        private MarkdownSearchResult result() {
            return result;
        }

        private double score() {
            return score;
        }

        private int firstSeenOrder() {
            return firstSeenOrder;
        }
    }
}
