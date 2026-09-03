package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;

import java.util.Objects;

public final class HybridRetrievalEvaluationService {

    private static final int EVALUATION_TOP_K = 5;
    private static final double DENSE_CANDIDATE_THRESHOLD = 0.0;
    private static final double ANSWER_SIMILARITY_THRESHOLD = 0.60;

    private final FixedRagEvaluationDataset dataset;
    private final MarkdownSimilaritySearchService denseSearchService;
    private final MarkdownSparseSearchService sparseSearchService;
    private final MarkdownHybridSearchService hybridSearchService;

    public HybridRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                            MarkdownSimilaritySearchService denseSearchService,
                                            MarkdownSparseSearchService sparseSearchService,
                                            MarkdownHybridSearchService hybridSearchService) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.denseSearchService = Objects.requireNonNull(denseSearchService, "向量检索服务不能为空");
        this.sparseSearchService = Objects.requireNonNull(sparseSearchService, "BM25 检索服务不能为空");
        this.hybridSearchService = Objects.requireNonNull(hybridSearchService, "混合检索服务不能为空");
    }

    public HybridRetrievalEvaluationReport evaluate() {
        RagRetrievalEvaluationService evaluator = new RagRetrievalEvaluationService(dataset,
                denseSearchService);
        RagRetrievalEvaluationReport dense = evaluator.evaluate();
        RagRetrievalEvaluationReport sparse = evaluator.evaluateWith(query ->
                sparseSearchService.search(query, EVALUATION_TOP_K),
                RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);
        RagRetrievalEvaluationReport hybrid = evaluator.evaluateWith(query ->
                hybridSearchService.search(query, EVALUATION_TOP_K, DENSE_CANDIDATE_THRESHOLD),
                RagRetrievalRejectionPolicy.COSINE_THRESHOLD);
        return new HybridRetrievalEvaluationReport(dense, sparse, hybrid,
                "Dense/Hybrid 使用余弦阈值；Sparse 先执行 technical_anchor_coverage_v1，再使用候选存在性。"
                        + "BM25 分数与余弦不在同一尺度，三策略拒答指标不能直接按分数数值比较。");
    }
}
