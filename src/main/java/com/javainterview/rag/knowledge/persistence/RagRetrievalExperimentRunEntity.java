package com.javainterview.rag.knowledge.persistence;

import java.time.LocalDateTime;

public class RagRetrievalExperimentRunEntity {
    private String runId;
    private String name;
    private String strategy;
    private String datasetVersion;
    private String datasetSplit;
    private String corpusHash;
    private String indexContract;
    private String gitSha;
    private String contractHash;
    private String status;
    private Integer totalCaseCount;
    private String caseResultsJson;
    private String reportJson;
    private String errorType;
    private String baselineRunId;
    private String decision;
    private String decisionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime decidedAt;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getDatasetVersion() { return datasetVersion; }
    public void setDatasetVersion(String datasetVersion) { this.datasetVersion = datasetVersion; }
    public String getDatasetSplit() { return datasetSplit; }
    public void setDatasetSplit(String datasetSplit) { this.datasetSplit = datasetSplit; }
    public String getCorpusHash() { return corpusHash; }
    public void setCorpusHash(String corpusHash) { this.corpusHash = corpusHash; }
    public String getIndexContract() { return indexContract; }
    public void setIndexContract(String indexContract) { this.indexContract = indexContract; }
    public String getGitSha() { return gitSha; }
    public void setGitSha(String gitSha) { this.gitSha = gitSha; }
    public String getContractHash() { return contractHash; }
    public void setContractHash(String contractHash) { this.contractHash = contractHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalCaseCount() { return totalCaseCount; }
    public void setTotalCaseCount(Integer totalCaseCount) { this.totalCaseCount = totalCaseCount; }
    public String getCaseResultsJson() { return caseResultsJson; }
    public void setCaseResultsJson(String caseResultsJson) { this.caseResultsJson = caseResultsJson; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String reportJson) { this.reportJson = reportJson; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getBaselineRunId() { return baselineRunId; }
    public void setBaselineRunId(String baselineRunId) { this.baselineRunId = baselineRunId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getDecisionNote() { return decisionNote; }
    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
