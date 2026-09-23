package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 将同一份 Chunk 同步交给向量索引和稀疏索引。 */
public final class CompositeMarkdownChunkIndexer implements GenerationAwareMarkdownChunkIndexer {

    private final List<MarkdownChunkIndexer> delegates;

    public CompositeMarkdownChunkIndexer(List<MarkdownChunkIndexer> delegates) {
        this.delegates = List.copyOf(Objects.requireNonNull(delegates, "Chunk 索引器不能为空"));
        if (this.delegates.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 Chunk 索引器");
        }
    }

    @Override
    public void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
        delegates.forEach(delegate -> delegate.index(document, chunks));
    }

    @Override
    public void buildGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                MarkdownIndexBuild build) {
        for (MarkdownChunkIndexer delegate : delegates) {
            generationAware(delegate).buildGeneration(document, chunks, build);
        }
    }

    @Override
    public void verifyGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                 MarkdownIndexBuild build) {
        for (MarkdownChunkIndexer delegate : delegates) {
            generationAware(delegate).verifyGeneration(document, chunks, build);
        }
    }

    @Override
    public void deleteGeneration(MarkdownDocument document, MarkdownIndexBuild build) {
        runAll(delegate -> generationAware(delegate).deleteGeneration(document, build));
    }

    @Override
    public void deleteOtherGenerations(MarkdownDocument document,
                                       MarkdownIndexBuild publishedBuild) {
        runAll(delegate -> generationAware(delegate)
                .deleteOtherGenerations(document, publishedBuild));
    }

    @Override
    public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        delegates.forEach(delegate -> delegate.rebuild(document, chunks));
    }

    @Override
    public void deleteOtherVersions(MarkdownDocument document) {
        runAll(delegate -> delegate.deleteOtherVersions(document));
    }

    @Override
    public void deleteVersion(MarkdownDocument document) {
        runAll(delegate -> delegate.deleteVersion(document));
    }

    @Override
    public void deleteBySource(String source) {
        runAll(delegate -> delegate.deleteBySource(source));
    }

    private void runAll(Consumer<MarkdownChunkIndexer> operation) {
        RuntimeException failure = null;
        for (MarkdownChunkIndexer delegate : delegates) {
            try {
                operation.accept(delegate);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private GenerationAwareMarkdownChunkIndexer generationAware(MarkdownChunkIndexer delegate) {
        if (delegate instanceof GenerationAwareMarkdownChunkIndexer generationAware) {
            return generationAware;
        }
        return new GenerationAwareMarkdownChunkIndexer() {
            @Override
            public void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
                delegate.index(document, chunks);
            }

            @Override
            public void buildGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                        MarkdownIndexBuild build) {
                delegate.index(document, chunks);
            }

            @Override
            public void deleteGeneration(MarkdownDocument document, MarkdownIndexBuild build) {
                delegate.deleteVersion(document);
            }

            @Override
            public void deleteOtherGenerations(MarkdownDocument document,
                                               MarkdownIndexBuild publishedBuild) {
                delegate.deleteOtherVersions(document);
            }
        };
    }
}
