package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import org.springframework.dao.DuplicateKeyException;

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
    public boolean unregister(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        return mapper.deleteBySourceAndVersion(document.source(), document.version()) == 1;
    }
}
