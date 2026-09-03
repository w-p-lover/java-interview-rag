package com.javainterview.rag.knowledge.evaluation;

public enum RagAnswerPointDiagnosisStatus {
    SUPPORTED_CITED_LITERAL,
    PRESENT_UNCITED,
    PARAPHRASE_CANDIDATE,
    NOT_RECOGNIZED,
    NO_ANSWER_REFUSAL
}
