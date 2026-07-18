package com.javainterview.rag.knowledge.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class MarkdownDocument {

    private final String source;
    private final String content;
    private final String version;

    private MarkdownDocument(String source, String content, String version) {
        this.source = source;
        this.content = content;
        this.version = version;
    }

    public static MarkdownDocument create(String source, String content) {
        Objects.requireNonNull(source, "文档来源不能为空");
        if (source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        Objects.requireNonNull(content, "文档内容不能为空");

        return new MarkdownDocument(source, content, calculateVersion(content));
    }

    public String source() {
        return source;
    }

    public String content() {
        return content;
    }

    public String version() {
        return version;
    }

    private static String calculateVersion(String content) {
        String normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }
}
