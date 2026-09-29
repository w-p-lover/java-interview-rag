package com.javainterview.rag.interview.evaluation;

import java.util.List;

public record AnswerExperimentReport(double passRate, double groundedAccuracy,
                                     double citationCompleteness, double averageOverallScore,
                                     double latencyP95Ms, int chatCallCount) {
    public static AnswerExperimentReport from(List<AnswerExperimentCaseResult> results) {
        List<AnswerExperimentCaseResult> ok = results.stream()
                .filter(result -> result.status() == AnswerExperimentCaseResult.Status.SUCCEEDED).toList();
        if (ok.isEmpty()) return new AnswerExperimentReport(0, 0, 0, 0, 0, 0);
        double pass = ok.stream().filter(AnswerExperimentCaseResult::passed).count() / (double) ok.size();
        List<AnswerExperimentCaseResult> judged = ok.stream()
                .filter(result -> result.actualKnowledgeGrounded() != null).toList();
        double grounded = judged.stream().filter(result -> result.expectedKnowledgeGrounded()
                == Boolean.TRUE.equals(result.actualKnowledgeGrounded())).count() / (double) Math.max(1, judged.size());
        double citations = judged.stream().filter(result -> !result.expectedKnowledgeGrounded()
                ? result.knowledgeEvidenceCount() == 0
                : result.knowledgeEvidenceCount() != null && result.knowledgeEvidenceCount() > 0).count()
                / (double) Math.max(1, judged.size());
        double average = ok.stream().filter(result -> result.overallScore() != null)
                .mapToInt(AnswerExperimentCaseResult::overallScore).average().orElse(0);
        List<Long> latencies = ok.stream().map(AnswerExperimentCaseResult::latencyMs).sorted().toList();
        double p95 = latencies.get(Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * .95) - 1));
        long chatCalls = ok.stream().filter(result -> result.overallScore() != null).count();
        return new AnswerExperimentReport(pass, grounded, citations, average, p95, (int) chatCalls);
    }
}
