package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.document.GenerationAwareMarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.MarkdownIndexBuild;
import com.javainterview.rag.knowledge.consistency.KnowledgeVectorIndexInspector;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QdrantMarkdownChunkIndexer implements GenerationAwareMarkdownChunkIndexer {

    private static final int EMBEDDING_BATCH_SIZE = 10;
    private static final Pattern PDF_PAGE_TITLE = Pattern.compile("^第 (\\d+) 页$");

    private final VectorStore vectorStore;
    private final KnowledgeIndexVersionMetadata indexVersion;
    private final KnowledgeVectorIndexInspector indexInspector;

    public QdrantMarkdownChunkIndexer(VectorStore vectorStore) {
        this(vectorStore, KnowledgeIndexVersionMetadata.current(), null);
    }

    public QdrantMarkdownChunkIndexer(VectorStore vectorStore,
                                      KnowledgeIndexVersionMetadata indexVersion) {
        this(vectorStore, indexVersion, null);
    }

    public QdrantMarkdownChunkIndexer(VectorStore vectorStore,
                                      KnowledgeIndexVersionMetadata indexVersion,
                                      KnowledgeVectorIndexInspector indexInspector) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "向量存储不能为空");
        this.indexVersion = Objects.requireNonNull(indexVersion, "索引版本元数据不能为空");
        this.indexInspector = indexInspector;
    }

    @Override
    public void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
        write(document, chunks, null);
    }

    @Override
    public void buildGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                MarkdownIndexBuild build) {
        write(document, chunks, Objects.requireNonNull(build, "索引构建信息不能为空"));
    }

    private void write(MarkdownDocument document, List<MarkdownChunk> chunks,
                       MarkdownIndexBuild build) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        if (chunks.isEmpty()) {
            return;
        }

        List<Document> vectorDocuments = chunks.stream()
                .map(chunk -> toVectorDocument(document, chunk, build))
                .toList();
        for (int start = 0; start < vectorDocuments.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, vectorDocuments.size());
            vectorStore.add(vectorDocuments.subList(start, end));
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("知识资料索引任务已超时，停止后续向量写入");
            }
        }
    }

    @Override
    public void deleteGeneration(MarkdownDocument document, MarkdownIndexBuild build) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.and(builder.eq("source", document.source()),
                        builder.eq("document_version", document.version())),
                builder.eq("index_generation", build.generation())
        ).build());
    }

    @Override
    public void verifyGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                 MarkdownIndexBuild build) {
        if (indexInspector == null) {
            return;
        }
        long actual = indexInspector.countGeneration(
                document.source(), document.version(), build.generation());
        if (actual != chunks.size()) {
            throw new IllegalStateException("Qdrant 临时代际 Chunk 数量不一致：expected="
                    + chunks.size() + ", actual=" + actual);
        }
    }

    @Override
    public void deleteOtherGenerations(MarkdownDocument document, MarkdownIndexBuild publishedBuild) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq("source", document.source()),
                builder.ne("index_generation", publishedBuild.generation())
        ).build());
    }

    @Override
    public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        if (chunks.isEmpty()) {
            deleteBySource(document.source());
            return;
        }

        index(document, chunks);
        deleteOtherVersions(document);
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.and(
                        builder.eq("source", document.source()),
                        builder.eq("document_version", document.version())
                ),
                builder.gte("chunk_order", chunks.size())
        ).build());
    }

    @Override
    public void deleteOtherVersions(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq("source", document.source()),
                builder.ne("document_version", document.version())
        ).build());
    }

    @Override
    public void deleteVersion(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq("source", document.source()),
                builder.eq("document_version", document.version())
        ).build());
    }

    @Override
    public void deleteBySource(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("source", source).build());
    }

    private Document toVectorDocument(MarkdownDocument document, MarkdownChunk chunk,
                                      MarkdownIndexBuild build) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", document.source());
        metadata.put("document_version", document.version());
        metadata.put("chunk_order", chunk.order());
        metadata.put("section_order", chunk.sectionOrder());
        metadata.put("heading_level", chunk.headingLevel());
        metadata.put("heading_path", chunk.headingPath());
        Integer pageNumber = chunk.pageNumber() != null
                ? chunk.pageNumber() : pageNumber(chunk.headingPath());
        metadata.put("document_format", pageNumber == null ? "MARKDOWN" : "PDF");
        metadata.put("block_type", chunk.blockType().name());
        metadata.put("embedding_model", indexVersion.embeddingModel());
        metadata.put("embedding_dimensions", indexVersion.embeddingDimensions());
        metadata.put("chunk_policy_version", indexVersion.chunkPolicyVersion());
        metadata.put("parser_version", indexVersion.parserVersion());
        metadata.put("index_schema_version", indexVersion.indexSchemaVersion());
        if (build != null) {
            metadata.put("index_generation", build.generation());
            metadata.put("index_temporary", true);
        }
        if (pageNumber != null) {
            metadata.put("page_number", pageNumber);
        }
        return Document.builder()
                .id(stableDocumentId(document, chunk, build))
                .text(chunk.content())
                .metadata(metadata)
                .build();
    }

    private Integer pageNumber(List<String> headingPath) {
        for (String title : headingPath) {
            Matcher matcher = PDF_PAGE_TITLE.matcher(title);
            if (matcher.matches()) {
                return Integer.valueOf(matcher.group(1));
            }
        }
        return null;
    }

    private String stableDocumentId(MarkdownDocument document, MarkdownChunk chunk,
                                    MarkdownIndexBuild build) {
        String generation = build == null ? "legacy" : build.generation();
        String identity = document.source() + '\0' + document.version() + '\0'
                + generation + '\0' + chunk.order();
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
