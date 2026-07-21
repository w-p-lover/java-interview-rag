package com.javainterview.rag.knowledge.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MarkdownFileImporter {

    private final MarkdownContentImporter contentImporter;

    public MarkdownFileImporter(MarkdownImportRegistry registry, int maxChunkLength) {
        this.contentImporter = new MarkdownContentImporter(registry, maxChunkLength);
    }

    public MarkdownImportResult importFile(Path file, String source) throws IOException {
        Objects.requireNonNull(file, "Markdown 文件路径不能为空");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return contentImporter.importContent(source, content);
    }
}
