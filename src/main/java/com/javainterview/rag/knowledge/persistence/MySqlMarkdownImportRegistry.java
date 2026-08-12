package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Objects;

public final class MySqlMarkdownImportRegistry implements MarkdownImportRegistry {

    private final KnowledgeDocumentVersionMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "文档版本 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public boolean register(MarkdownDocument document) {
        return register(document, null);
    }

    @Override
    public boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        KnowledgeDocumentVersionEntity entity = KnowledgeDocumentVersionEntity.create(
                document.source(), document.version(), document.content().length(), writeOutline(outline));
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
