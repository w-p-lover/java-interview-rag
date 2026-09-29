package com.javainterview.rag.interview.evaluation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ScoreStabilityContractProvider {
    public static final int REPEAT_COUNT = 2;
    private final FixedInterviewAnswerEvaluationDataset dataset;
    private final InterviewAnswerEvaluator evaluator;

    public ScoreStabilityContractProvider(FixedInterviewAnswerEvaluationDataset dataset,
                                          InterviewAnswerEvaluator evaluator) {
        this.dataset = dataset;
        this.evaluator = evaluator;
    }

    public Contract current() {
        String hash = sha256(String.join("|", "answer-score-stability-v1",
                evaluator.promptVersion(), evaluator.rubricVersion(), evaluator.modelName(),
                Integer.toString(dataset.findAll().size()), Integer.toString(REPEAT_COUNT)));
        return new Contract("answer-score-stability-v1", evaluator.promptVersion(),
                evaluator.rubricVersion(), evaluator.modelName(), hash,
                dataset.findAll().size(), REPEAT_COUNT);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    public record Contract(String datasetVersion, String promptVersion, String rubricVersion,
                           String modelName, String contractHash, int totalCaseCount,
                           int repeatCount) { }
}
