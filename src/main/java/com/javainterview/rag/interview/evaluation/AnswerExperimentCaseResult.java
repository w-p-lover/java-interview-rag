package com.javainterview.rag.interview.evaluation;

public record AnswerExperimentCaseResult(String caseId, Status status, boolean passed,
                                         boolean expectedKnowledgeGrounded,
                                         Boolean actualKnowledgeGrounded, Integer overallScore,
                                         Integer answerEvidenceCount, Integer knowledgeEvidenceCount,
                                         Long latencyMs, String errorType) {
    public enum Status { SUCCEEDED, FAILED }
}
