package com.javainterview.rag.knowledge.evaluation;

public enum RagAnswerPointDiagnosisStatus {
    SUPPORTED_CITED_LITERAL,
    PRESENT_UNCITED,
    PARAPHRASE_CANDIDATE,
    RETRIEVED_NOT_USED,
    RETRIEVAL_EVIDENCE_MISSING,
    NO_ANSWER_REFUSAL
}
