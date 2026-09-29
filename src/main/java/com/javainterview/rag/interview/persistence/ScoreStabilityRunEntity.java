package com.javainterview.rag.interview.persistence;

import java.time.LocalDateTime;

public class ScoreStabilityRunEntity {
    private String runId, name, datasetVersion, promptVersion, rubricVersion, modelName, contractHash,
            status, caseResultsJson, reportJson, errorType, baselineRunId, decision, decisionNote;
    private Integer totalCaseCount, repeatCount;
    private LocalDateTime createdAt, updatedAt, completedAt, decidedAt;
    public String getRunId() { return runId; } public void setRunId(String value) { runId = value; }
    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDatasetVersion() { return datasetVersion; } public void setDatasetVersion(String value) { datasetVersion = value; }
    public String getPromptVersion() { return promptVersion; } public void setPromptVersion(String value) { promptVersion = value; }
    public String getRubricVersion() { return rubricVersion; } public void setRubricVersion(String value) { rubricVersion = value; }
    public String getModelName() { return modelName; } public void setModelName(String value) { modelName = value; }
    public String getContractHash() { return contractHash; } public void setContractHash(String value) { contractHash = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public String getCaseResultsJson() { return caseResultsJson; } public void setCaseResultsJson(String value) { caseResultsJson = value; }
    public String getReportJson() { return reportJson; } public void setReportJson(String value) { reportJson = value; }
    public String getErrorType() { return errorType; } public void setErrorType(String value) { errorType = value; }
    public String getBaselineRunId() { return baselineRunId; } public void setBaselineRunId(String value) { baselineRunId = value; }
    public String getDecision() { return decision; } public void setDecision(String value) { decision = value; }
    public String getDecisionNote() { return decisionNote; } public void setDecisionNote(String value) { decisionNote = value; }
    public Integer getTotalCaseCount() { return totalCaseCount; } public void setTotalCaseCount(Integer value) { totalCaseCount = value; }
    public Integer getRepeatCount() { return repeatCount; } public void setRepeatCount(Integer value) { repeatCount = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime value) { completedAt = value; }
    public LocalDateTime getDecidedAt() { return decidedAt; } public void setDecidedAt(LocalDateTime value) { decidedAt = value; }
}
