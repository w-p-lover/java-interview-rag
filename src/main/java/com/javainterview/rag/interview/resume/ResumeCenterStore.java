package com.javainterview.rag.interview.resume;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResumeCenterStore {

    List<ResumeRecord> findAll();

    Optional<ResumeRecord> findRecord(String id);

    Optional<ResumeVersion> findVersion(String id);

    List<ResumeVersion> findVersions(String resumeId);

    Optional<ResumeVersion> findVersionByHash(String contentHash);

    SaveVersionResult saveVersion(SaveVersionRequest request);

    boolean switchCurrent(String resumeId, String versionId,
                          long expectedVersion, LocalDateTime updatedAt);

    record SaveVersionRequest(
            String resumeId,
            String name,
            String targetRole,
            String contentHash,
            String originalFilename,
            String contentType,
            long fileSize,
            String fullText,
            LocalDateTime createdAt
    ) { }

    record SaveVersionResult(
            ResumeRecord record,
            ResumeVersion version,
            boolean duplicate
    ) { }
}
