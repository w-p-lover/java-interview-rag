package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 在可靠初始命中后，补充同来源、同版本的相邻 Chunk。
 */
public final class MarkdownAdjacentChunkExpander {

    private static final int MAX_NEIGHBOR_WINDOW = 1;
    private static final int MAX_EXPANSION_SEEDS = 3;
    private static final int MAX_EXPANDED_RESULTS = 8;

    private final VectorStore vectorStore;

    public MarkdownAdjacentChunkExpander(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "向量存储不能为空");
    }

    public List<MarkdownSearchResult> expand(List<MarkdownSearchResult> initialResults) {
        return expandDetailed(initialResults).results();
    }

    public ExpansionResult expandDetailed(List<MarkdownSearchResult> initialResults) {
        Objects.requireNonNull(initialResults, "初始检索结果不能为空");
        if (initialResults.isEmpty()) {
            return new ExpansionResult(List.of(), 0);
        }

        Map<ChunkIdentity, MarkdownSearchResult> expanded = new LinkedHashMap<>();
        initialResults.stream()
                .map(result -> Objects.requireNonNull(result, "检索结果不能为空"))
                .forEach(result -> expanded.putIfAbsent(ChunkIdentity.from(result), result));

        List<NeighborCandidate> candidates = new ArrayList<>();
        List<MarkdownSearchResult> seeds = expanded.values().stream()
                .limit(MAX_EXPANSION_SEEDS)
                .toList();
        for (MarkdownSearchResult seed : seeds) {
            candidates.addAll(findNeighbors(seed));
        }
        candidates.stream()
                .sorted(Comparator.comparingInt(NeighborCandidate::distance)
                        .thenComparingInt(NeighborCandidate::seedOrder)
                        .thenComparingInt(candidate -> candidate.result().chunkOrder()))
                .forEach(candidate -> expanded.putIfAbsent(
                        ChunkIdentity.from(candidate.result()), candidate.result()));

        return new ExpansionResult(expanded.values().stream()
                .limit(MAX_EXPANDED_RESULTS)
                .toList(), seeds.size());
    }

    private List<NeighborCandidate> findNeighbors(MarkdownSearchResult seed) {
        int minOrder = Math.max(0, seed.chunkOrder() - MAX_NEIGHBOR_WINDOW);
        int maxOrder = seed.chunkOrder() + MAX_NEIGHBOR_WINDOW;
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(seed.content())
                .topK(MAX_NEIGHBOR_WINDOW * 2 + 1)
                .similarityThresholdAll()
                .filterExpression(builder.and(
                        builder.and(
                                builder.eq("source", seed.source()),
                                builder.eq("document_version", seed.documentVersion())
                        ),
                        builder.and(
                                builder.gte("chunk_order", minOrder),
                                builder.lte("chunk_order", maxOrder)
                        )
                ).build())
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toSearchResult)
                .filter(candidate -> candidate.source().equals(seed.source())
                        && candidate.documentVersion().equals(seed.documentVersion())
                        && candidate.chunkOrder() >= minOrder
                        && candidate.chunkOrder() <= maxOrder)
                .filter(candidate -> !ChunkIdentity.from(candidate).equals(ChunkIdentity.from(seed)))
                .map(candidate -> new NeighborCandidate(
                        candidate,
                        Math.abs(candidate.chunkOrder() - seed.chunkOrder()),
                        seed.chunkOrder()))
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

    private record ChunkIdentity(String source, String documentVersion, int chunkOrder) {

        private static ChunkIdentity from(MarkdownSearchResult result) {
            return new ChunkIdentity(result.source(), result.documentVersion(), result.chunkOrder());
        }
    }

    private record NeighborCandidate(MarkdownSearchResult result, int distance, int seedOrder) {
    }

    public record ExpansionResult(List<MarkdownSearchResult> results, int vectorQueryCount) {

        public ExpansionResult {
            results = List.copyOf(Objects.requireNonNull(results, "扩展结果不能为空"));
            if (vectorQueryCount < 0) {
                throw new IllegalArgumentException("向量查询次数不能小于 0");
            }
        }
    }
}
