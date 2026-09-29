package com.javainterview.rag.interview.evaluation;

import java.util.List;

public record ScoreStabilityReport(
        double passRate,
        double groundedAccuracy,
        double stableCaseRate,
        double averageScore,
        double averageScoreRange,
        double latencyP95Ms,
        int chatCallCount) {

    public static ScoreStabilityReport from(List<ScoreStabilityCaseResult> results, int repeatCount) {
        List<ScoreStabilityCaseResult> complete = results.stream()
                .filter(result -> result.status() == ScoreStabilityCaseResult.Status.SUCCEEDED
                        && result.complete(repeatCount)).toList();
        if (complete.isEmpty()) {
            return new ScoreStabilityReport(0, 0, 0, 0, 0, 0, 0);
        }
        double pass = complete.stream().filter(ScoreStabilityCaseResult::passed).count()
                / (double) complete.size();
        double grounded = complete.stream()
                .filter(result -> !result.knowledgeGroundedResults().isEmpty())
                .filter(result -> result.knowledgeGroundedResults().stream()
                        .allMatch(actual -> actual == result.expectedKnowledgeGrounded())).count()
                / (double) Math.max(1, complete.stream()
                        .filter(result -> !result.knowledgeGroundedResults().isEmpty()).count());
        double stable = complete.stream().filter(result -> result.overallScores().isEmpty()
                || range(result.overallScores()) <= 10).count() / (double) complete.size();
        double average = complete.stream().filter(result -> !result.overallScores().isEmpty())
                .flatMap(result -> result.overallScores().stream()).mapToInt(Integer::intValue)
                .average().orElse(0);
        double averageRange = complete.stream().filter(result -> !result.overallScores().isEmpty())
                .mapToInt(result -> range(result.overallScores())).average().orElse(0);
        List<Long> latencies = complete.stream().map(ScoreStabilityCaseResult::latencyMs).sorted().toList();
        double p95 = latencies.get(Math.min(latencies.size() - 1,
                (int) Math.ceil(latencies.size() * .95) - 1));
        int calls = complete.stream().mapToInt(result -> result.overallScores().size()).sum();
        return new ScoreStabilityReport(pass, grounded, stable, average, averageRange, p95, calls);
    }

    private static int range(List<Integer> scores) {
        return scores.stream().mapToInt(Integer::intValue).max().orElse(0)
                - scores.stream().mapToInt(Integer::intValue).min().orElse(0);
    }
}
