package com.javainterview.rag.file;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FileAssetStore {

    Optional<FileAsset> find(String id);

    Optional<FileAsset> findByPurposeAndHash(FilePurpose purpose, String contentHash);

    FileAsset createUploading(FileAsset asset);

    boolean markUploading(String id, String originalFilename, String contentType,
                          long fileSize, LocalDateTime updatedAt);

    boolean markReady(String id, LocalDateTime updatedAt);

    boolean markFailed(String id, String errorType, LocalDateTime updatedAt);

    boolean markDeletePending(String id, LocalDateTime updatedAt);

    boolean markDeleted(String id, LocalDateTime deletedAt);

    int countBusinessReferences(String id);
}
