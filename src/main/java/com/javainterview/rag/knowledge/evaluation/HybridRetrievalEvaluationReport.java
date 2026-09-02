package com.javainterview.rag.knowledge.evaluation;

import java.util.Objects;

public record HybridRetrievalEvaluationReport(
        RagRetrievalEvaluationReport dense,
        RagRetrievalEvaluationReport sparse,
        RagRetrievalEvaluationReport hybrid,
        String rejectionMetricNote
) {

    public HybridRetrievalEvaluationReport {
        Objects.requireNonNull(dense, "Dense 评估结果不能为空");
        Objects.requireNonNull(sparse, "Sparse 评估结果不能为空");
        Objects.requireNonNull(hybrid, "Hybrid 评估结果不能为空");
        Objects.requireNonNull(rejectionMetricNote, "拒答指标说明不能为空");
    }
}
