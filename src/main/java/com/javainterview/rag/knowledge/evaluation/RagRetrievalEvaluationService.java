package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;

import java.util.List;
import java.util.Objects;

public class RagRetrievalEvaluationService {

    private static final int EVALUATION_TOP_K = 5;
    private static final double EVALUATION_SIMILARITY_THRESHOLD = 0.0;

    private final FixedRagEvaluationDataset dataset;
    private final MarkdownSimilaritySearchService searchService;

    public RagRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                         MarkdownSimilaritySearchService searchService) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.searchService = Objects.requireNonNull(searchService, "相似度检索服务不能为空");
    }

    public RagRetrievalEvaluationReport evaluate() {
        List<RagRetrievalEvaluationCaseResult> results = dataset.findAll().stream()
                .map(this::evaluateCase)
                .toList();
        int expectedSourceHitCount = (int) results.stream()
                .filter(RagRetrievalEvaluationCaseResult::expectedSourceMatched)
                .count();
        double expectedSourceHitRate = results.isEmpty() ? 0.0 : (double) expectedSourceHitCount / results.size();
        return new RagRetrievalEvaluationReport(EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD,
                results.size(), expectedSourceHitCount, expectedSourceHitRate, results);
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase) {
        List<RagRetrievalSource> actualSources = searchService.search(evaluationCase.question(),
                        EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD)
                .stream()
                .map(RagRetrievalSource::from)
                .toList();
        boolean expectedSourceMatched = actualSources.stream()
                .map(RagRetrievalSource::source)
                .anyMatch(evaluationCase.expectedSources()::contains);
        return new RagRetrievalEvaluationCaseResult(evaluationCase.id(), evaluationCase.question(),
                evaluationCase.expectedSources(), actualSources, expectedSourceMatched);
    }
}
