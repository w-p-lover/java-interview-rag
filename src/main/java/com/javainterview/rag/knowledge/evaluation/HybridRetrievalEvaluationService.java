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
                sparseSearchService.search(query, EVALUATION_TOP_K), ANSWER_SIMILARITY_THRESHOLD);
        RagRetrievalEvaluationReport hybrid = evaluator.evaluateWith(query ->
                hybridSearchService.search(query, EVALUATION_TOP_K, DENSE_CANDIDATE_THRESHOLD),
                ANSWER_SIMILARITY_THRESHOLD);
        return new HybridRetrievalEvaluationReport(dense, sparse, hybrid,
                "Dense 使用余弦阈值；BM25 分数与余弦不在同一尺度，Sparse/Hybrid 的拒答指标只表示候选为空，不能直接与 Dense 阈值作数值比较。");
    }
}
