package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record RagRetrievalExperimentRun(
        String runId,
        String name,
        RagRetrievalExperimentStrategy strategy,
        String datasetVersion,
        RagRetrievalExperimentSplit datasetSplit,
        String corpusHash,
        String indexContract,
        String gitSha,
        String contractHash,
        RagRetrievalExperimentStatus status,
        int totalCaseCount,
        List<RagRetrievalExperimentCaseResult> caseResults,
        RagRetrievalEvaluationReport report,
        String errorType,
        String baselineRunId,
        RagRetrievalExperimentDecision decision,
        String decisionNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime decidedAt
) {
    public RagRetrievalExperimentRun {
        runId = requireText(runId, "实验运行 ID 不能为空");
        name = requireText(name, "实验名称不能为空");
        Objects.requireNonNull(strategy, "检索策略不能为空");
        datasetVersion = requireText(datasetVersion, "数据集版本不能为空");
        Objects.requireNonNull(datasetSplit, "数据集切分不能为空");
        corpusHash = requireText(corpusHash, "语料指纹不能为空");
        indexContract = requireText(indexContract, "索引契约不能为空");
        gitSha = requireText(gitSha, "Git SHA 不能为空");
        contractHash = requireText(contractHash, "运行契约指纹不能为空");
        Objects.requireNonNull(status, "实验运行状态不能为空");
        if (totalCaseCount <= 0) {
            throw new IllegalArgumentException("实验样本数必须大于 0");
        }
        caseResults = List.copyOf(Objects.requireNonNull(caseResults, "逐样本结果不能为空"));
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");
    }

    public boolean terminal() {
        return status == RagRetrievalExperimentStatus.SUCCEEDED
                || status == RagRetrievalExperimentStatus.PARTIAL_FAILED;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
