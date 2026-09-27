package com.javainterview.rag.file;

import java.time.LocalDateTime;
import java.util.Objects;

public record FileAsset(
        String id,
        FilePurpose purpose,
        String objectKey,
        String contentHash,
        String originalFilename,
        String contentType,
        long fileSize,
        FileAssetStatus status,
        String errorType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public FileAsset {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("文件 ID 不能为空");
        }
        purpose = Objects.requireNonNull(purpose, "文件用途不能为空");
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("文件对象键不能为空");
        }
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("文件 Hash 格式无效");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("原始文件名不能为空");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (fileSize < 1 || fileSize > FileAssetService.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小无效");
        }
        status = Objects.requireNonNull(status, "文件状态不能为空");
        createdAt = Objects.requireNonNull(createdAt, "文件创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "文件更新时间不能为空");
    }
}
