package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Optional;

public interface ResumeAnalysisResultStore {

    ResumeAnalysisResult saveSuccessful(ResumeAnalysisResult result);

    Optional<ResumeAnalysisResult> findLatest(
            String resumeVersionId, ResumeAnalysisType analysisType);

    List<ResumeAnalysisResult> findAll(String resumeVersionId);
}
