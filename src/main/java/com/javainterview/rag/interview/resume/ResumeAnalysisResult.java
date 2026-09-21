package com.javainterview.rag.interview.resume;

import java.time.LocalDateTime;
import java.util.Objects;

public record ResumeAnalysisResult(
        String id,
        String resumeVersionId,
        ResumeAnalysisType analysisType,
        String inputFingerprint,
        String contractVersion,
        String modelName,
        String resultJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ResumeAnalysisResult {
        id = ResumeRecord.text(id, 36, "简历分析结果 ID");
        resumeVersionId = ResumeRecord.text(resumeVersionId, 36, "简历版本 ID");
        Objects.requireNonNull(analysisType, "简历分析类型不能为空");
        inputFingerprint = ResumeRecord.text(inputFingerprint, 64, "简历分析输入指纹");
        if (!inputFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("简历分析输入指纹格式无效");
        }
        contractVersion = ResumeRecord.text(contractVersion, 100, "简历分析契约版本");
        modelName = ResumeRecord.text(modelName, 100, "简历分析模型名称");
        if (resultJson == null || resultJson.isBlank()) {
            throw new IllegalArgumentException("简历分析结果不能为空");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("简历分析结果时间不能为空");
        }
    }
}
