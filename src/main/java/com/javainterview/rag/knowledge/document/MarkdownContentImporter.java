package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public final class MarkdownContentImporter {

    private final MarkdownImportRegistry registry;
    private final MarkdownHeadingParser headingParser = new MarkdownHeadingParser();
    private final MarkdownSectionChunker sectionChunker = new MarkdownSectionChunker();
    private final KnowledgeDocumentOutlineBuilder outlineBuilder = new KnowledgeDocumentOutlineBuilder();
    private final MarkdownChunkIndexer chunkIndexer;
    private final MarkdownIndexingStateStore indexingStateStore;
    private final int maxChunkLength;

    public MarkdownContentImporter(MarkdownImportRegistry registry, int maxChunkLength) {
        this(registry, maxChunkLength, MarkdownChunkIndexer.noOp());
    }

    public MarkdownContentImporter(MarkdownImportRegistry registry, int maxChunkLength,
                                   MarkdownChunkIndexer chunkIndexer) {
        this(registry, maxChunkLength, chunkIndexer, null);
    }

    public MarkdownContentImporter(MarkdownImportRegistry registry, int maxChunkLength,
                                   MarkdownChunkIndexer chunkIndexer,
                                   MarkdownIndexingStateStore indexingStateStore) {
        this.registry = Objects.requireNonNull(registry, "Markdown 导入登记器不能为空");
        if (maxChunkLength <= 0) {
            throw new IllegalArgumentException("Chunk 最大长度必须大于 0");
        }
        this.chunkIndexer = Objects.requireNonNull(chunkIndexer, "Markdown Chunk 索引器不能为空");
        this.indexingStateStore = indexingStateStore;
        this.maxChunkLength = maxChunkLength;
    }

    public MarkdownImportResult importContent(String source, String content) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        List<MarkdownSection> sections = headingParser.parse(document);
        return importDocument(document, sections);
    }

    public MarkdownImportResult importStructuredContent(String source, String content,
                                                        List<MarkdownSection> sections) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        return importDocument(document, validateSections(document, sections));
    }

    private MarkdownImportResult importDocument(MarkdownDocument document,
                                                List<MarkdownSection> sections) {
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        KnowledgeDocumentOutline outline = outlineBuilder.build(sections, chunks);
        if (!registry.register(document, outline, sections)) {
            retryFailedCleanup(document);
            return new MarkdownImportResult(document, false, List.of());
        }
        try {
            chunkIndexer.index(document, chunks);
        } catch (RuntimeException exception) {
            compensateFailedIndex(document, exception);
            try {
                registry.unregister(document);
            } catch (RuntimeException unregisterException) {
                exception.addSuppressed(unregisterException);
            }
            throw exception;
        }
        chunkIndexer.deleteOtherVersions(document);
        return new MarkdownImportResult(document, true, chunks);
    }

    private void compensateFailedIndex(MarkdownDocument document, RuntimeException failure) {
        try {
            chunkIndexer.deleteVersion(document);
        } catch (RuntimeException compensationException) {
            failure.addSuppressed(compensationException);
        }
    }

    private void retryFailedCleanup(MarkdownDocument document) {
        if (indexingStateStore == null) {
            return;
        }
        boolean currentVersion = registry.findVersionsBySource(document.source()).stream()
                .findFirst()
                .map(version -> version.version().equals(document.version()))
                .orElse(false);
        boolean cleanupFailed = indexingStateStore.find(document.source(), document.version())
                .map(state -> state.status() == MarkdownIndexingStatus.FAILED)
                .orElse(false);
        if (currentVersion && cleanupFailed) {
            chunkIndexer.deleteOtherVersions(document);
        }
    }

    public MarkdownRebuildResult rebuildContent(String source, String content) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        return rebuildDocument(document, headingParser.parse(document));
    }

    public MarkdownRebuildResult rebuildStructuredContent(String source, String content,
                                                           List<MarkdownSection> sections) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        return rebuildDocument(document, validateSections(document, sections));
    }

    public MarkdownRebuildResult rebuildCurrentSnapshot(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        List<KnowledgeDocumentSummary> versions = registry.findVersionsBySource(source);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("资料不存在");
        }
        KnowledgeDocumentSummary current = versions.get(0);
        KnowledgeDocumentSnapshot snapshot = registry.findSnapshot(source, current.version())
                .orElseThrow(() -> new IllegalArgumentException(
                        "当前版本没有可恢复快照，请重新上传原文件"));
        List<MarkdownSection> sections = snapshot.sections().isEmpty()
                ? headingParser.parse(snapshot.document())
                : validateSections(snapshot.document(), snapshot.sections());
        return rebuildDocument(snapshot.document(), sections);
    }

    public MarkdownRebuildResult rollbackToVersion(String source, String version) {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(version, "文档版本不能为空");
        List<KnowledgeDocumentSummary> versions = registry.findVersionsBySource(source);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (versions.get(0).version().equals(version)) {
            throw new IllegalArgumentException("目标版本已经是当前版本");
        }
        KnowledgeDocumentSnapshot snapshot = registry.findSnapshot(source, version)
                .orElseThrow(() -> new IllegalArgumentException("该历史版本没有可恢复快照，请重新上传原文件"));
        List<MarkdownSection> sections = snapshot.sections().isEmpty()
                ? headingParser.parse(snapshot.document())
                : validateSections(snapshot.document(), snapshot.sections());
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        try {
            chunkIndexer.rebuild(snapshot.document(), chunks);
            registry.updateOutline(snapshot.document(), outlineBuilder.build(sections, chunks));
            if (!registry.activate(source, version)) {
                throw new IllegalStateException("历史版本激活失败");
            }
            boolean activated = registry.findVersionsBySource(source).stream()
                    .findFirst()
                    .map(current -> current.version().equals(version))
                    .orElse(false);
            if (!activated) {
                throw new IllegalStateException("历史版本激活后未成为当前版本");
            }
        } catch (RuntimeException exception) {
            if (indexingStateStore != null) {
                indexingStateStore.markFailed(snapshot.document(), chunks.size(), exception);
            }
            throw exception;
        }
        return new MarkdownRebuildResult(snapshot.document(), chunks);
    }

    private MarkdownRebuildResult rebuildDocument(MarkdownDocument document,
                                                   List<MarkdownSection> sections) {
        List<KnowledgeDocumentSummary> versions = registry.findVersionsBySource(document.source());
        boolean registered = versions.stream()
                .anyMatch(version -> version.version().equals(document.version()));
        if (!registered) {
            throw new IllegalArgumentException("文档版本尚未导入，不能重建");
        }
        if (!versions.get(0).version().equals(document.version())) {
            throw new IllegalArgumentException("只能重建当前版本，历史版本不能直接恢复为检索版本");
        }
        List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
        try {
            chunkIndexer.rebuild(document, chunks);
            registry.updateOutline(document, outlineBuilder.build(sections, chunks));
        } catch (RuntimeException exception) {
            // 章节目录更新失败时，不能让索引状态继续保持 READY；下次巡检必须暴露这次重建未完整收口。
            if (indexingStateStore != null) {
                indexingStateStore.markFailed(document, chunks.size(), exception);
            }
            throw exception;
        }
        return new MarkdownRebuildResult(document, chunks);
    }

    private List<MarkdownSection> validateSections(MarkdownDocument document,
                                                   List<MarkdownSection> sections) {
        Objects.requireNonNull(sections, "结构化章节不能为空");
        List<MarkdownSection> copy = List.copyOf(sections);
        for (int index = 0; index < copy.size(); index++) {
            MarkdownSection section = Objects.requireNonNull(copy.get(index), "结构化章节不能为空");
            if (!section.source().equals(document.source())) {
                throw new IllegalArgumentException("结构化章节来源必须与文档来源一致");
            }
            if (section.order() != index) {
                throw new IllegalArgumentException("结构化章节顺序必须连续且从 0 开始");
            }
        }
        return copy;
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
