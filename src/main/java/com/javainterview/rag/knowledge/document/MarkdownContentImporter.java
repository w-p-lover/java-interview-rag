package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public final class MarkdownContentImporter {

    private final MarkdownImportRegistry registry;
    private final MarkdownHeadingParser headingParser = new MarkdownHeadingParser();
    private final MarkdownSectionChunker sectionChunker = new MarkdownSectionChunker();
    private final MarkdownChunkIndexer chunkIndexer;
    private final int maxChunkLength;

    public MarkdownContentImporter(MarkdownImportRegistry registry, int maxChunkLength) {
        this(registry, maxChunkLength, MarkdownChunkIndexer.noOp());
    }

    public MarkdownContentImporter(MarkdownImportRegistry registry, int maxChunkLength,
                                   MarkdownChunkIndexer chunkIndexer) {
        this.registry = Objects.requireNonNull(registry, "Markdown 导入登记器不能为空");
        if (maxChunkLength <= 0) {
            throw new IllegalArgumentException("Chunk 最大长度必须大于 0");
        }
        this.chunkIndexer = Objects.requireNonNull(chunkIndexer, "Markdown Chunk 索引器不能为空");
        this.maxChunkLength = maxChunkLength;
    }

    public MarkdownImportResult importContent(String source, String content) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        if (!registry.register(document)) {
            return new MarkdownImportResult(document, false, List.of());
        }

        List<MarkdownSection> sections = headingParser.parse(document);
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        try {
            chunkIndexer.index(document, chunks);
        } catch (RuntimeException exception) {
            try {
                registry.unregister(document);
            } catch (RuntimeException unregisterException) {
                exception.addSuppressed(unregisterException);
            }
            throw exception;
        }
        return new MarkdownImportResult(document, true, chunks);
    }
}
