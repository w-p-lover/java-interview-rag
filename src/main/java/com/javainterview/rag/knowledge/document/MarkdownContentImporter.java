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
            chunkIndexer.deleteOtherVersions(document);
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

    public MarkdownRebuildResult rebuildContent(String source, String content) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        List<KnowledgeDocumentSummary> versions = registry.findVersionsBySource(document.source());
        boolean registered = versions.stream()
                .anyMatch(version -> version.version().equals(document.version()));
        if (!registered) {
            throw new IllegalArgumentException("文档版本尚未导入，不能重建");
        }
        if (!versions.get(0).version().equals(document.version())) {
            throw new IllegalArgumentException("只能重建当前版本，历史版本不能直接恢复为检索版本");
        }

        List<MarkdownSection> sections = headingParser.parse(document);
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        chunkIndexer.rebuild(document, chunks);
        return new MarkdownRebuildResult(document, chunks);
    }

    public boolean deleteBySource(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        if (source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
        chunkIndexer.deleteBySource(source);
        return registry.unregisterBySource(source);
    }
}
