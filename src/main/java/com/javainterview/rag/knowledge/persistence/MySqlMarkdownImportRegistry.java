package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Objects;

public final class MySqlMarkdownImportRegistry implements MarkdownImportRegistry {

    private final KnowledgeDocumentVersionMapper mapper;

    public MySqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "文档版本 Mapper 不能为空");
    }

    @Override
    public boolean register(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        KnowledgeDocumentVersionEntity entity = KnowledgeDocumentVersionEntity.create(
                document.source(), document.version(), document.content().length());
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
    public List<KnowledgeDocumentSummary> findCurrentDocuments() {
        return mapper.selectCurrentVersions().stream()
                .map(entity -> new KnowledgeDocumentSummary(
                        entity.getSource(), entity.getVersion(), entity.getContentLength(), entity.getCreatedAt()))
                .toList();
    }
}
