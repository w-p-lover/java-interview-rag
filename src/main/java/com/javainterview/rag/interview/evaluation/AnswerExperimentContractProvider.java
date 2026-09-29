package com.javainterview.rag.interview.evaluation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AnswerExperimentContractProvider {
    private final FixedInterviewAnswerEvaluationDataset dataset;
    private final InterviewAnswerEvaluator evaluator;
    public AnswerExperimentContractProvider(FixedInterviewAnswerEvaluationDataset dataset, InterviewAnswerEvaluator evaluator) {
        this.dataset = dataset; this.evaluator = evaluator;
    }
    public Contract current() {
        String hash = sha256(String.join("|", "answer-evaluation-v1", evaluator.promptVersion(),
                evaluator.rubricVersion(), evaluator.modelName(), Integer.toString(dataset.findAll().size())));
        return new Contract("answer-evaluation-v1", evaluator.promptVersion(), evaluator.rubricVersion(),
                evaluator.modelName(), hash, dataset.findAll().size());
    }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 不可用", e); } }
    public record Contract(String datasetVersion, String promptVersion, String rubricVersion,
                           String modelName, String contractHash, int totalCaseCount) { }
}
