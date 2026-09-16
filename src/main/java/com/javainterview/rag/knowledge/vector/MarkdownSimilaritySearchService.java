package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MarkdownSimilaritySearchService {

    private static final int MAX_TOP_K = 20;

    private final VectorStore vectorStore;
    private final KnowledgeRetrievalDeadline deadline;

    public MarkdownSimilaritySearchService(VectorStore vectorStore) {
        this(vectorStore, null);
    }

    public MarkdownSimilaritySearchService(VectorStore vectorStore,
                                           KnowledgeRetrievalDeadline deadline) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "向量存储不能为空");
        this.deadline = deadline;
    }

    public List<MarkdownSearchResult> search(String query, int topK, double similarityThreshold) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK 必须在 1 到 20 之间");
        }
        if (!Double.isFinite(similarityThreshold)
                || similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("相似度阈值必须在 0 到 1 之间");
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        List<Document> documents = deadline == null
                ? vectorStore.similaritySearch(request)
                : deadline.execute("Dense 向量检索", () -> vectorStore.similaritySearch(request));
        return documents.stream()
                .map(this::toSearchResult)
                .toList();
    }

    private MarkdownSearchResult toSearchResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new MarkdownSearchResult(
                Objects.requireNonNull(document.getText(), "检索结果正文不能为空"),
                requiredString(metadata, "source"),
                requiredString(metadata, "document_version"),
                requiredInt(metadata, "chunk_order"),
                requiredStringList(metadata, "heading_path"),
                document.getScore(),
                optionalInt(metadata, "page_number"),
                blockType(metadata)
        );
    }

    private String requiredString(Map<String, Object> metadata, String key) {
        Object value = Objects.requireNonNull(metadata.get(key), "检索结果缺少元数据：" + key);
        return value.toString();
    }

    private int requiredInt(Map<String, Object> metadata, String key) {
        Object value = Objects.requireNonNull(metadata.get(key), "检索结果缺少元数据：" + key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("检索结果元数据类型错误：" + key);
    }

    private Integer optionalInt(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("检索结果元数据类型错误：" + key);
    }

    private KnowledgeBlockType blockType(Map<String, Object> metadata) {
        Object value = metadata.get("block_type");
        if (value == null) {
            return KnowledgeBlockType.TEXT;
        }
        try {
            return KnowledgeBlockType.valueOf(value.toString());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("检索结果元数据类型错误：block_type", exception);
        }
    }

    private List<String> requiredStringList(Map<String, Object> metadata, String key) {
        Object value = Objects.requireNonNull(metadata.get(key), "检索结果缺少元数据：" + key);
        if (value instanceof List<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        throw new IllegalStateException("检索结果元数据类型错误：" + key);
    }
}
