package com.javainterview.rag.file.persistence;

import com.javainterview.rag.file.FileAsset;
import com.javainterview.rag.file.FileAssetStatus;
import com.javainterview.rag.file.FileAssetStore;
import com.javainterview.rag.file.FilePurpose;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Optional;

public final class MySqlFileAssetStore implements FileAssetStore {

    private final FileAssetMapper mapper;

    public MySqlFileAssetStore(FileAssetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<FileAsset> find(String id) {
        return Optional.ofNullable(mapper.find(id)).map(this::toDomain);
    }

    @Override
    public Optional<FileAsset> findByPurposeAndHash(FilePurpose purpose, String contentHash) {
        return Optional.ofNullable(mapper.findByPurposeAndHash(purpose.name(), contentHash))
                .map(this::toDomain);
    }

    @Override
    public FileAsset createUploading(FileAsset asset) {
        try {
            mapper.insert(toEntity(asset));
            return asset;
        } catch (DuplicateKeyException exception) {
            return findByPurposeAndHash(asset.purpose(), asset.contentHash()).orElseThrow(() -> exception);
        }
    }

    @Override
    public boolean markUploading(String id, String originalFilename, String contentType,
                                 long fileSize, LocalDateTime updatedAt) {
        return mapper.markUploading(id, originalFilename, contentType, fileSize, updatedAt) == 1;
    }

    @Override
    public boolean markReady(String id, LocalDateTime updatedAt) {
        return mapper.markReady(id, updatedAt) == 1;
    }

    @Override
    public boolean markFailed(String id, String errorType, LocalDateTime updatedAt) {
        return mapper.markFailed(id, errorType, updatedAt) == 1;
    }

    @Override
    public boolean markDeletePending(String id, LocalDateTime updatedAt) {
        return mapper.markDeletePending(id, updatedAt) == 1;
    }

    @Override
    public boolean markDeleted(String id, LocalDateTime deletedAt) {
        return mapper.markDeleted(id, deletedAt) == 1;
    }

    @Override
    public int countBusinessReferences(String id) {
        return mapper.countBusinessReferences(id);
    }

    private FileAsset toDomain(FileAssetEntity entity) {
        return new FileAsset(entity.getId(), FilePurpose.valueOf(entity.getPurpose()),
                entity.getObjectKey(), entity.getContentHash(), entity.getOriginalFilename(),
                entity.getContentType(), entity.getFileSize(), FileAssetStatus.valueOf(entity.getStatus()),
                entity.getErrorType(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }

    private FileAssetEntity toEntity(FileAsset asset) {
        return new FileAssetEntity(asset.id(), asset.purpose().name(), asset.objectKey(),
                asset.contentHash(), asset.originalFilename(), asset.contentType(), asset.fileSize(),
                asset.status().name(), asset.errorType(), asset.createdAt(), asset.updatedAt(), asset.deletedAt());
    }
}
