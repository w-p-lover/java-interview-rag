package com.javainterview.rag.knowledge.evaluation;

import java.util.Objects;

public record RagAnswerPointDiagnosis(
        String expectedAnswerPoint,
        RagAnswerPointDiagnosisStatus status
) {

    public RagAnswerPointDiagnosis {
        Objects.requireNonNull(expectedAnswerPoint, "预期回答要点不能为空");
        Objects.requireNonNull(status, "回答要点诊断状态不能为空");
    }
}
