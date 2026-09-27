package com.javainterview.rag.file;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryFileAssetStore implements FileAssetStore {

    private final Map<String, FileAsset> files = new LinkedHashMap<>();

    @Override
    public synchronized Optional<FileAsset> find(String id) {
        return Optional.ofNullable(files.get(id));
    }

    @Override
    public synchronized Optional<FileAsset> findByPurposeAndHash(FilePurpose purpose, String contentHash) {
        return files.values().stream()
                .filter(file -> file.purpose() == purpose && file.contentHash().equals(contentHash))
                .findFirst();
    }

    @Override
    public synchronized FileAsset createUploading(FileAsset asset) {
        FileAsset existing = findByPurposeAndHash(asset.purpose(), asset.contentHash()).orElse(null);
        if (existing != null) {
            return existing;
        }
        files.put(asset.id(), asset);
        return asset;
    }

    @Override
    public synchronized boolean markUploading(String id, String originalFilename, String contentType,
                                              long fileSize, LocalDateTime updatedAt) {
        FileAsset current = files.get(id);
        if (current == null) {
            return false;
        }
        files.put(id, copy(current, originalFilename, contentType, fileSize,
                FileAssetStatus.UPLOADING, null, updatedAt, null));
        return true;
    }

    @Override
    public synchronized boolean markReady(String id, LocalDateTime updatedAt) {
        return updateStatus(id, FileAssetStatus.READY, null, updatedAt, null);
    }

    @Override
    public synchronized boolean markFailed(String id, String errorType, LocalDateTime updatedAt) {
        return updateStatus(id, FileAssetStatus.FAILED, errorType, updatedAt, null);
    }

    @Override
    public synchronized boolean markDeletePending(String id, LocalDateTime updatedAt) {
        FileAsset current = files.get(id);
        if (current == null || current.status() != FileAssetStatus.READY) {
            return false;
        }
        return updateStatus(id, FileAssetStatus.DELETE_PENDING, null, updatedAt, null);
    }

    @Override
    public synchronized boolean markDeleted(String id, LocalDateTime deletedAt) {
        return updateStatus(id, FileAssetStatus.DELETED, null, deletedAt, deletedAt);
    }

    @Override
    public int countBusinessReferences(String id) {
        return 0;
    }

    private boolean updateStatus(String id, FileAssetStatus status, String errorType,
                                 LocalDateTime updatedAt, LocalDateTime deletedAt) {
        FileAsset current = files.get(id);
        if (current == null) {
            return false;
        }
        files.put(id, copy(current, current.originalFilename(), current.contentType(), current.fileSize(),
                status, errorType, updatedAt, deletedAt));
        return true;
    }

    private FileAsset copy(FileAsset file, String filename, String contentType, long fileSize,
                           FileAssetStatus status, String errorType,
                           LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new FileAsset(file.id(), file.purpose(), file.objectKey(), file.contentHash(),
                filename, contentType, fileSize, status, errorType,
                file.createdAt(), updatedAt, deletedAt);
    }
}
