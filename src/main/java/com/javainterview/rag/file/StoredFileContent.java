package com.javainterview.rag.file;

import java.util.Arrays;
import java.util.Objects;

public record StoredFileContent(FileAsset asset, byte[] content) {
    public StoredFileContent {
        asset = Objects.requireNonNull(asset, "文件元数据不能为空");
        content = Arrays.copyOf(Objects.requireNonNull(content, "文件内容不能为空"), content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
