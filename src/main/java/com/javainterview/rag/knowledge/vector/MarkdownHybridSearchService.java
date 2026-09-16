package com.javainterview.rag.knowledge.vector;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 负责 Dense 与 BM25 双路候选召回及 RRF 排名融合。 */
public final class MarkdownHybridSearchService {

    private static final int MAX_TOP_K = 20;
    private static final int CANDIDATE_MULTIPLIER = 4;
    private static final int RRF_RANK_CONSTANT = 60;

    private final MarkdownSimilaritySearchService denseSearchService;
    private final MarkdownSparseSearchService sparseSearchService;

    public MarkdownHybridSearchService(MarkdownSimilaritySearchService denseSearchService,
                                       MarkdownSparseSearchService sparseSearchService) {
        this.denseSearchService = Objects.requireNonNull(denseSearchService, "向量检索服务不能为空");
        this.sparseSearchService = Objects.requireNonNull(sparseSearchService, "BM25 检索服务不能为空");
    }

    public List<MarkdownSearchResult> search(String query, int topK, double denseSimilarityThreshold) {
        return searchDetailed(query, topK, denseSimilarityThreshold).results();
    }

    public MarkdownHybridSearchResult searchDetailed(String query, int topK,
                                                     double denseSimilarityThreshold) {
        validate(query, topK, denseSimilarityThreshold);
        int candidateTopK = Math.min(MAX_TOP_K, Math.max(topK, topK * CANDIDATE_MULTIPLIER));
        long startedAt = System.nanoTime();
        List<MarkdownSearchResult> denseResults;
        RuntimeException denseFailure = null;
        try {
            denseResults = denseSearchService.search(
                    query, candidateTopK, denseSimilarityThreshold);
        } catch (RuntimeException exception) {
            denseResults = List.of();
            denseFailure = exception;
        }
        List<MarkdownSearchResult> sparseResults;
        RuntimeException sparseFailure = null;
        try {
            sparseResults = sparseSearchService.search(query, candidateTopK);
        } catch (RuntimeException exception) {
            sparseResults = List.of();
            sparseFailure = exception;
        }
        if (denseFailure != null && sparseFailure != null) {
            KnowledgeRetrievalUnavailableException unavailable =
                    new KnowledgeRetrievalUnavailableException("Dense 与 Sparse 检索均不可用", sparseFailure);
            unavailable.addSuppressed(denseFailure);
            throw unavailable;
        }

        Map<ChunkIdentity, Candidate> candidates = new LinkedHashMap<>();
        for (int index = 0; index < denseResults.size(); index++) {
            MarkdownSearchResult result = denseResults.get(index);
            candidates.computeIfAbsent(ChunkIdentity.from(result), ignored -> new Candidate(result))
                    .acceptDense(result, index + 1);
        }
        for (int index = 0; index < sparseResults.size(); index++) {
            MarkdownSearchResult result = sparseResults.get(index);
            candidates.computeIfAbsent(ChunkIdentity.from(result), ignored -> new Candidate(result))
                    .acceptSparse(result, index + 1);
        }

        List<Candidate> ranked = candidates.values().stream()
                .sorted(Comparator.comparingDouble(Candidate::rrfScore).reversed()
                        .thenComparingInt(Candidate::firstSeenOrder))
                .limit(topK)
                .toList();
        List<MarkdownHybridSearchHit> hits = ranked.stream()
                .map(Candidate::toHit)
                .toList();
        RetrievalStrategy strategy = denseFailure != null
                ? RetrievalStrategy.SPARSE_ONLY
                : sparseFailure != null ? RetrievalStrategy.DENSE_ONLY : RetrievalStrategy.HYBRID;
        RuntimeException degradation = denseFailure != null ? denseFailure : sparseFailure;
        return new MarkdownHybridSearchResult(query, topK, denseResults.size(), sparseResults.size(),
                hits, (System.nanoTime() - startedAt) / 1_000_000, strategy,
                degradation != null, degradation == null ? null : degradation.getClass().getSimpleName());
    }

    private void validate(String query, int topK, double denseSimilarityThreshold) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK 必须在 1 到 20 之间");
        }
        if (!Double.isFinite(denseSimilarityThreshold)
                || denseSimilarityThreshold < 0.0 || denseSimilarityThreshold > 1.0) {
            throw new IllegalArgumentException("Dense 相似度阈值必须在 0 到 1 之间");
        }
    }

    public record MarkdownHybridSearchResult(String query, int topK, int denseCandidateCount,
                                             int sparseCandidateCount, List<MarkdownHybridSearchHit> hits,
                                             long elapsedMillis, RetrievalStrategy strategy,
                                             boolean degraded, String degradationReason) {

        public MarkdownHybridSearchResult(String query, int topK, int denseCandidateCount,
                                          int sparseCandidateCount,
                                          List<MarkdownHybridSearchHit> hits,
                                          long elapsedMillis) {
            this(query, topK, denseCandidateCount, sparseCandidateCount, hits, elapsedMillis,
                    RetrievalStrategy.HYBRID, false, null);
        }

        public MarkdownHybridSearchResult {
            Objects.requireNonNull(query, "检索问题不能为空");
            hits = List.copyOf(Objects.requireNonNull(hits, "融合结果不能为空"));
            strategy = Objects.requireNonNull(strategy, "检索策略不能为空");
            if (degraded != (strategy != RetrievalStrategy.HYBRID)) {
                throw new IllegalArgumentException("检索降级状态与策略不一致");
            }
            if (degraded && (degradationReason == null || degradationReason.isBlank())) {
                throw new IllegalArgumentException("检索降级原因不能为空");
            }
            if (!degraded) {
                degradationReason = null;
            }
        }

        public List<MarkdownSearchResult> results() {
            return hits.stream().map(MarkdownHybridSearchHit::result).toList();
        }
    }

    public enum RetrievalStrategy {
        HYBRID,
        DENSE_ONLY,
        SPARSE_ONLY
    }

    public record MarkdownHybridSearchHit(MarkdownSearchResult result, Integer denseRank,
                                          Integer sparseRank, Double denseScore, Double sparseScore,
                                          double rrfScore) {

        public MarkdownHybridSearchHit {
            Objects.requireNonNull(result, "融合结果不能为空");
        }
    }

    private record ChunkIdentity(String source, String documentVersion, int chunkOrder) {

        private static ChunkIdentity from(MarkdownSearchResult result) {
            return new ChunkIdentity(result.source(), result.documentVersion(), result.chunkOrder());
        }
    }

    private static final class Candidate {

        private final MarkdownSearchResult firstResult;
        private int firstSeenOrder;
        private Integer denseRank;
        private Integer sparseRank;
        private Double denseScore;
        private Double sparseScore;

        private Candidate(MarkdownSearchResult firstResult) {
            this.firstResult = firstResult;
            this.firstSeenOrder = Integer.MAX_VALUE;
        }

        private void acceptDense(MarkdownSearchResult result, int rank) {
            firstSeenOrder = Math.min(firstSeenOrder, rank);
            denseRank = rank;
            denseScore = result.similarity();
        }

        private void acceptSparse(MarkdownSearchResult result, int rank) {
            firstSeenOrder = Math.min(firstSeenOrder, rank);
            sparseRank = rank;
            sparseScore = result.similarity();
        }

        private double rrfScore() {
            double score = 0.0;
            if (denseRank != null) {
                score += 1.0 / (RRF_RANK_CONSTANT + denseRank);
            }
            if (sparseRank != null) {
                score += 1.0 / (RRF_RANK_CONSTANT + sparseRank);
            }
            return score;
        }

        private int firstSeenOrder() {
            return firstSeenOrder;
        }

        private MarkdownHybridSearchHit toHit() {
            return new MarkdownHybridSearchHit(firstResult, denseRank, sparseRank,
                    denseScore, sparseScore, rrfScore());
        }
    }
}
