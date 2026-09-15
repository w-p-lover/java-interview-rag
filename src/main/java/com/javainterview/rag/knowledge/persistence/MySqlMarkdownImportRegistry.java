package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MySqlMarkdownImportRegistry implements MarkdownImportRegistry {
    private static final TypeReference<List<MarkdownSection>> SECTIONS = new TypeReference<>() { };

    private final KnowledgeDocumentVersionMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "文档版本 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline,
                            List<MarkdownSection> sections) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        KnowledgeDocumentVersionEntity entity = KnowledgeDocumentVersionEntity.create(
                document.source(), document.version(), document.content().length(), writeOutline(outline),
                document.content(), writeSections(sections));
        try {
            return mapper.insert(entity) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        return mapper.countBySourceAndVersion(document.source(), document.version()) > 0;
    }

    @Override
    public boolean unregister(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        return mapper.deleteBySourceAndVersion(document.source(), document.version()) == 1;
    }

    @Override
    public boolean unregisterBySource(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        return mapper.deleteBySource(source) > 0;
    }

    @Override
    public void updateOutline(MarkdownDocument document, KnowledgeDocumentOutline outline) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(outline, "章节目录不能为空");
        if (mapper.updateOutline(document.source(), document.version(), writeOutline(outline)) != 1) {
            throw new IllegalArgumentException("文档版本尚未登记，不能更新章节目录");
        }
    }

    @Override
    public List<KnowledgeDocumentSummary> findCurrentDocuments() {
        return mapper.selectCurrentVersions().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<KnowledgeDocumentSummary> findVersionsBySource(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        return mapper.selectVersionsBySource(source).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Optional<KnowledgeDocumentSnapshot> findSnapshot(String source, String version) {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(version, "文档版本不能为空");
        KnowledgeDocumentVersionEntity entity = mapper.selectBySourceAndVersion(source, version);
        if (entity == null || entity.getContent() == null || entity.getSectionsJson() == null) {
            return Optional.empty();
        }
        try {
            MarkdownDocument document = MarkdownDocument.create(entity.getSource(), entity.getContent());
            if (!document.version().equals(entity.getVersion())) {
                throw new IllegalStateException("文档快照版本校验失败");
            }
            return Optional.of(new KnowledgeDocumentSnapshot(document,
                    objectMapper.readValue(entity.getSectionsJson(), SECTIONS)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("文档章节快照数据损坏", exception);
        }
    }

    @Override
    public boolean hasSnapshot(String source, String version) {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(version, "文档版本不能为空");
        return mapper.countSnapshotBySourceAndVersion(source, version) > 0;
    }

    @Override
    public boolean activate(String source, String version) {
        return mapper.activate(source, version) == 1;
    }

    private KnowledgeDocumentSummary toSummary(KnowledgeDocumentVersionEntity entity) {
        return new KnowledgeDocumentSummary(
                entity.getSource(), entity.getVersion(), entity.getContentLength(), entity.getCreatedAt(),
                readOutline(entity.getOutlineJson()));
    }

    private String writeOutline(KnowledgeDocumentOutline outline) {
        if (outline == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(outline);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("章节目录序列化失败", exception);
        }
    }

    private String writeSections(List<MarkdownSection> sections) {
        try {
            return objectMapper.writeValueAsString(sections == null ? List.of() : sections);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("文档章节快照序列化失败", exception);
        }
    }

    private KnowledgeDocumentOutline readOutline(String outlineJson) {
        if (outlineJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(outlineJson, KnowledgeDocumentOutline.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("章节目录数据损坏", exception);
        }
    }
}
