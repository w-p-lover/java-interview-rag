package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownIndexPublication;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 以状态表中的发布指针隔离正在构建和已失败的索引代际。 */
public final class PublishedMarkdownIndexFilter {

    private final MarkdownIndexingStateStore stateStore;

    public PublishedMarkdownIndexFilter(MarkdownIndexingStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore, "索引状态存储不能为空");
    }

    public Filter.Expression allPublished() {
        return snapshot().allPublished();
    }

    public Filter.Expression publishedFor(String source, String documentVersion) {
        return snapshot().publishedFor(source, documentVersion);
    }

    public boolean isPublished(Document document) {
        return snapshot().isPublished(document);
    }

    public Snapshot snapshot() {
        return new Snapshot(stateStore.findPublished());
    }

    public final class Snapshot {

        private final List<MarkdownIndexPublication> publications;

        private Snapshot(List<MarkdownIndexPublication> publications) {
            this.publications = List.copyOf(publications);
        }

        public Filter.Expression allPublished() {
            return combine(publications);
        }

        public Filter.Expression publishedFor(String source, String documentVersion) {
            return combine(publications.stream()
                    .filter(publication -> publication.source().equals(source)
                            && publication.documentVersion().equals(documentVersion))
                    .toList());
        }

        public boolean isPublished(Document document) {
            Map<String, Object> metadata = document.getMetadata();
            String source = string(metadata, "source");
            String version = string(metadata, "document_version");
            Object generation = metadata.get("index_generation");
            return publications.stream().anyMatch(publication ->
                    publication.source().equals(source)
                            && publication.documentVersion().equals(version)
                            && (publication.legacy()
                            ? generation == null
                            : publication.generation().equals(Objects.toString(generation, null))));
        }
    }

    private Filter.Expression combine(List<MarkdownIndexPublication> publications) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        if (publications.isEmpty()) {
            return builder.eq("source", "__no_published_index__").build();
        }
        Filter.Expression combined = branch(builder, publications.get(0));
        for (int index = 1; index < publications.size(); index++) {
            combined = new Filter.Expression(Filter.ExpressionType.OR,
                    combined, branch(builder, publications.get(index)));
        }
        return combined;
    }

    private Filter.Expression branch(FilterExpressionBuilder builder,
                                     MarkdownIndexPublication publication) {
        Filter.Expression sourceVersion = builder.and(
                builder.eq("source", publication.source()),
                builder.eq("document_version", publication.documentVersion())).build();
        if (publication.legacy()) {
            if (publication.buildingGeneration() == null) {
                return sourceVersion;
            }
            return new Filter.Expression(Filter.ExpressionType.AND, sourceVersion,
                    builder.ne("index_generation", publication.buildingGeneration()).build());
        }
        return new Filter.Expression(Filter.ExpressionType.AND, sourceVersion,
                builder.eq("index_generation", publication.generation()).build());
    }

    private String string(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }
}
