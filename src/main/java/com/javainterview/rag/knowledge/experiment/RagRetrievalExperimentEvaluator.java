package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalRejectionPolicy;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;

import java.util.List;
import java.util.Objects;

public class RagRetrievalExperimentEvaluator {

    private static final int TOP_K = 5;
    private static final double DENSE_CANDIDATE_THRESHOLD = 0.0;
    private static final int DEV_CASE_COUNT = 20;

    private final RagRetrievalEvaluationService evaluationService;
    private final MarkdownSimilaritySearchService denseSearchService;
    private final MarkdownSparseSearchService sparseSearchService;
    private final MarkdownHybridSearchService hybridSearchService;

    public RagRetrievalExperimentEvaluator(
            RagRetrievalEvaluationService evaluationService,
            MarkdownSimilaritySearchService denseSearchService,
            MarkdownSparseSearchService sparseSearchService,
            MarkdownHybridSearchService hybridSearchService) {
        this.evaluationService = Objects.requireNonNull(evaluationService, "检索评估服务不能为空");
        this.denseSearchService = Objects.requireNonNull(denseSearchService, "Dense 检索服务不能为空");
        this.sparseSearchService = Objects.requireNonNull(sparseSearchService, "Sparse 检索服务不能为空");
        this.hybridSearchService = Objects.requireNonNull(hybridSearchService, "Hybrid 检索服务不能为空");
    }

    public String datasetVersion() {
        return FixedRagEvaluationDataset.VERSION;
    }

    public List<RagEvaluationCase> cases(RagRetrievalExperimentSplit split) {
        List<RagEvaluationCase> cases = evaluationService.evaluationCases();
        if (cases.size() <= DEV_CASE_COUNT) {
            throw new IllegalStateException("固定检索评估集不足以划分 DEV/HOLDOUT");
        }
        return split == RagRetrievalExperimentSplit.DEV
                ? List.copyOf(cases.subList(0, DEV_CASE_COUNT))
                : List.copyOf(cases.subList(DEV_CASE_COUNT, cases.size()));
    }

    public RagRetrievalEvaluationCaseResult evaluateCase(
            RagEvaluationCase evaluationCase,
            RagRetrievalExperimentStrategy strategy) {
        return switch (strategy) {
            case DENSE -> evaluationService.evaluateCase(evaluationCase,
                    query -> denseSearchService.search(query, TOP_K, DENSE_CANDIDATE_THRESHOLD),
                    RagRetrievalRejectionPolicy.COSINE_THRESHOLD);
            case SPARSE -> evaluationService.evaluateCase(evaluationCase,
                    query -> sparseSearchService.search(query, TOP_K),
                    RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);
            case HYBRID -> evaluationService.evaluateCase(evaluationCase,
                    query -> hybridSearchService.search(query, TOP_K, DENSE_CANDIDATE_THRESHOLD),
                    RagRetrievalRejectionPolicy.COSINE_THRESHOLD);
        };
    }

    public RagRetrievalEvaluationReport summarize(
            RagRetrievalExperimentStrategy strategy,
            List<RagRetrievalEvaluationCaseResult> results) {
        return evaluationService.summarize(results, rejectionPolicy(strategy));
    }

    private RagRetrievalRejectionPolicy rejectionPolicy(RagRetrievalExperimentStrategy strategy) {
        return strategy == RagRetrievalExperimentStrategy.SPARSE
                ? RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE
                : RagRetrievalRejectionPolicy.COSINE_THRESHOLD;
    }
}
