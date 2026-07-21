package com.javainterview.rag.knowledge.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class MarkdownFileImporter {

    private final MarkdownImportRegistry registry;
    private final MarkdownHeadingParser headingParser = new MarkdownHeadingParser();
    private final MarkdownSectionChunker sectionChunker = new MarkdownSectionChunker();
    private final int maxChunkLength;

    public MarkdownFileImporter(MarkdownImportRegistry registry, int maxChunkLength) {
        this.registry = Objects.requireNonNull(registry, "Markdown 导入登记器不能为空");
        if (maxChunkLength <= 0) {
            throw new IllegalArgumentException("Chunk 最大长度必须大于 0");
        }
        this.maxChunkLength = maxChunkLength;
    }

    public MarkdownImportResult importFile(Path file, String source) throws IOException {
        Objects.requireNonNull(file, "Markdown 文件路径不能为空");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        MarkdownDocument document = MarkdownDocument.create(source, content);
        if (!registry.register(document)) {
            return new MarkdownImportResult(document, false, List.of());
        }

        List<MarkdownSection> sections = headingParser.parse(document);
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        return new MarkdownImportResult(document, true, chunks);
    }
}
