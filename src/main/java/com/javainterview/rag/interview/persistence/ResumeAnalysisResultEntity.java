package com.javainterview.rag.interview.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResultEntity {
    private String id;
    private String resumeVersionId;
    private String analysisType;
    private String inputFingerprint;
    private String contractVersion;
    private String modelName;
    private String resultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
