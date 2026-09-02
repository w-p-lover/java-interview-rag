package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public record RagRetrievalEvaluationCaseResult(
        String evaluationCaseId,
        String question,
        RagEvaluationCaseType type,
        List<String> expectedSources,
        List<List<String>> expectedHeadingPaths,
        List<RagEvaluationStructureExpectation> expectedStructures,
        List<RagRetrievalSource> actualSources,
        boolean expectedSourceMatched,
        Integer firstRelevantRank,
        boolean hitAt1,
        double recallAt5,
        double reciprocalRank,
        int expectedHeadingPathHitCount,
        double headingPathRecall,
        int expectedPageNumberHitCount,
        double pageNumberRecall,
        int expectedBlockTypeHitCount,
        double blockTypeRecall,
        int expectedStructureHitCount,
        double structureRecall,
        boolean rejectionExpected,
        boolean rejectedAtAnswerThreshold,
        boolean rejectionCorrect,
        List<RagEvaluationChunkExpectation> expectedChunks,
        List<RagEvaluationChunkExpectation> hardNegativeChunks,
        double chunkRecallAt5,
        double precisionAt5,
        double ndcgAt5,
        double contextNoiseRate,
        double retrievalLatencyMs,
        double hardNegativeAvoidanceRate
) {

    public RagRetrievalEvaluationCaseResult(
            String evaluationCaseId,
            String question,
            RagEvaluationCaseType type,
            List<String> expectedSources,
            List<List<String>> expectedHeadingPaths,
            List<RagEvaluationStructureExpectation> expectedStructures,
            List<RagRetrievalSource> actualSources,
            boolean expectedSourceMatched,
            Integer firstRelevantRank,
            boolean hitAt1,
            double recallAt5,
            double reciprocalRank,
            int expectedHeadingPathHitCount,
            double headingPathRecall,
            int expectedPageNumberHitCount,
            double pageNumberRecall,
            int expectedBlockTypeHitCount,
            double blockTypeRecall,
            int expectedStructureHitCount,
            double structureRecall,
            boolean rejectionExpected,
            boolean rejectedAtAnswerThreshold,
            boolean rejectionCorrect) {
        this(evaluationCaseId, question, type, expectedSources, expectedHeadingPaths, expectedStructures,
                actualSources, expectedSourceMatched, firstRelevantRank, hitAt1, recallAt5, reciprocalRank,
                expectedHeadingPathHitCount, headingPathRecall, expectedPageNumberHitCount, pageNumberRecall,
                expectedBlockTypeHitCount, blockTypeRecall, expectedStructureHitCount, structureRecall,
                rejectionExpected, rejectedAtAnswerThreshold, rejectionCorrect,
                List.of(), List.of(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public RagRetrievalEvaluationCaseResult {
        Objects.requireNonNull(evaluationCaseId, "评估题标识不能为空");
        Objects.requireNonNull(question, "评估问题不能为空");
        Objects.requireNonNull(type, "评估题型不能为空");
        expectedSources = List.copyOf(Objects.requireNonNull(expectedSources, "期望来源不能为空"));
        expectedHeadingPaths = Objects.requireNonNull(expectedHeadingPaths, "期望标题路径不能为空").stream()
                .map(List::copyOf)
                .toList();
        expectedStructures = List.copyOf(Objects.requireNonNull(expectedStructures, "结构期望不能为空"));
        actualSources = List.copyOf(Objects.requireNonNull(actualSources, "实际来源不能为空"));
        expectedChunks = List.copyOf(Objects.requireNonNull(expectedChunks, "期望 Chunk 不能为空"));
        hardNegativeChunks = List.copyOf(Objects.requireNonNull(hardNegativeChunks, "Hard Negative Chunk 不能为空"));
    }
}
