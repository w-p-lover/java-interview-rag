package com.javainterview.rag.file;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

public final class LocalFileObjectStorage implements FileObjectStorage {

    private final Path root;

    public LocalFileObjectStorage(Path root) {
        this.root = Objects.requireNonNull(root, "本地文件根目录不能为空")
                .toAbsolutePath().normalize();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        Objects.requireNonNull(content, "文件内容不能为空");
        Path target = resolve(objectKey);
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new FileAssetUnavailableException("本地文件写入失败", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 正式写入结果已经确定，临时文件由后续巡检处理。
                }
            }
        }
    }

    @Override
    public byte[] read(String objectKey) {
        try {
            return Files.readAllBytes(resolve(objectKey));
        } catch (java.nio.file.NoSuchFileException exception) {
            throw new FileAssetUnavailableException("文件对象不存在");
        } catch (IOException exception) {
            throw new FileAssetUnavailableException("本地文件读取失败", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new FileAssetUnavailableException("本地文件删除失败", exception);
        }
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("文件对象键不能为空");
        }
        Path resolved = root.resolve(objectKey.replace('/', java.io.File.separatorChar)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("文件对象键不能越过存储根目录");
        }
        return resolved;
    }
}
