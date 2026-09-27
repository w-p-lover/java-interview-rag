package com.javainterview.rag.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public final class FileAssetService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final FileAssetStore store;
    private final FileObjectStorage objectStorage;

    public FileAssetService(FileAssetStore store, FileObjectStorage objectStorage) {
        this.store = Objects.requireNonNull(store, "文件元数据存储不能为空");
        this.objectStorage = Objects.requireNonNull(objectStorage, "文件对象存储不能为空");
    }

    public FileAsset store(FilePurpose purpose, String originalFilename,
                           String contentType, byte[] content) {
        Objects.requireNonNull(purpose, "文件用途不能为空");
        String filename = text(originalFilename, 255, "原始文件名");
        String normalizedContentType = text(
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
                100, "文件类型");
        byte[] bytes = Objects.requireNonNull(content, "文件内容不能为空");
        if (bytes.length < 1) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件不能超过 10 MiB");
        }
        String contentHash = hash(bytes);
        LocalDateTime now = LocalDateTime.now();
        String objectKey = purpose.objectPrefix() + "/" + contentHash.substring(0, 2) + "/" + contentHash;
        FileAsset existing = store.findByPurposeAndHash(purpose, contentHash).orElse(null);
        FileAsset asset = existing == null
                ? store.createUploading(new FileAsset(UUID.randomUUID().toString(), purpose, objectKey,
                contentHash, filename, normalizedContentType, bytes.length, FileAssetStatus.UPLOADING,
                null, now, now, null))
                : existing;
        if (asset.status() == FileAssetStatus.READY && objectStorage.exists(asset.objectKey())) {
            return asset;
        }
        if (!store.markUploading(asset.id(), filename, normalizedContentType, bytes.length, now)) {
            throw new IllegalStateException("文件元数据无法进入上传状态");
        }
        try {
            objectStorage.put(asset.objectKey(), bytes, normalizedContentType);
            if (!store.markReady(asset.id(), LocalDateTime.now())) {
                throw new IllegalStateException("文件元数据无法进入可用状态");
            }
            return require(asset.id());
        } catch (RuntimeException exception) {
            store.markFailed(asset.id(), exception.getClass().getSimpleName(), LocalDateTime.now());
            throw exception;
        }
    }

    public FileAsset require(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("文件 ID 不能为空");
        }
        return store.find(id.strip()).orElseThrow(() -> new NoSuchElementException("文件不存在"));
    }

    public StoredFileContent read(String id) {
        FileAsset asset = require(id);
        if (asset.status() != FileAssetStatus.READY) {
            throw new FileAssetUnavailableException("文件当前不可下载，状态为 " + asset.status());
        }
        if (!objectStorage.exists(asset.objectKey())) {
            throw new FileAssetUnavailableException("文件元数据存在，但对象内容缺失");
        }
        byte[] content = objectStorage.read(asset.objectKey());
        if (content.length != asset.fileSize() || !hash(content).equals(asset.contentHash())) {
            throw new FileAssetUnavailableException("文件对象大小或 Hash 与元数据不一致");
        }
        return new StoredFileContent(asset, content);
    }

    public FileAsset deleteIfUnreferenced(String id) {
        FileAsset asset = require(id);
        int references = store.countBusinessReferences(asset.id());
        if (references > 0) {
            throw new FileAssetConflictException("文件仍被 " + references + " 条业务记录引用，不能删除");
        }
        if (asset.status() == FileAssetStatus.DELETED) {
            return asset;
        }
        if (asset.status() == FileAssetStatus.READY
                && !store.markDeletePending(asset.id(), LocalDateTime.now())) {
            throw new FileAssetConflictException("文件状态已变化，请刷新后重试");
        }
        try {
            objectStorage.delete(asset.objectKey());
            if (!store.markDeleted(asset.id(), LocalDateTime.now())) {
                throw new IllegalStateException("文件元数据无法进入已删除状态");
            }
            return require(asset.id());
        } catch (RuntimeException exception) {
            store.markFailed(asset.id(), exception.getClass().getSimpleName(), LocalDateTime.now());
            throw exception;
        }
    }

    public FileConsistency inspect(String id) {
        FileAsset asset = require(id);
        boolean objectExists = objectStorage.exists(asset.objectKey());
        return new FileConsistency(asset, store.countBusinessReferences(id), objectExists,
                asset.status() == FileAssetStatus.READY && objectExists);
    }

    private String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private String text(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    public record FileConsistency(FileAsset file, int referenceCount,
                                  boolean objectExists, boolean consistent) { }
}
