package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class QdrantMarkdownChunkIndexer implements MarkdownChunkIndexer {

    private final VectorStore vectorStore;

    public QdrantMarkdownChunkIndexer(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "向量存储不能为空");
    }

    @Override
    public void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        if (chunks.isEmpty()) {
            return;
        }

        List<Document> vectorDocuments = chunks.stream()
                .map(chunk -> toVectorDocument(document, chunk))
                .toList();
        vectorStore.add(vectorDocuments);
    }

    private Document toVectorDocument(MarkdownDocument document, MarkdownChunk chunk) {
        return Document.builder()
                .id(stableDocumentId(document, chunk))
                .text(chunk.content())
                .metadata(Map.of(
                        "source", document.source(),
                        "document_version", document.version(),
                        "chunk_order", chunk.order(),
                        "section_order", chunk.sectionOrder(),
                        "heading_level", chunk.headingLevel(),
                        "heading_path", chunk.headingPath()
                ))
                .build();
    }

    private String stableDocumentId(MarkdownDocument document, MarkdownChunk chunk) {
        String identity = document.source() + '\0' + document.version() + '\0' + chunk.order();
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
