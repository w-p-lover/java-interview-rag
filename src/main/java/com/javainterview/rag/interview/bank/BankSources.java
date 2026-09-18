package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownHeadingParser;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownSectionChunker;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class BankSources {
    private final MarkdownImportRegistry registry;
    private final int maxChunkTokens;

    public BankSources(MarkdownImportRegistry registry, int maxChunkTokens) {
        this.registry = registry;
        this.maxChunkTokens = maxChunkTokens;
    }

    public String currentVersion(String source) {
        return registry.findVersionsBySource(source).stream().findFirst()
                .map(document -> document.version()).orElse(null);
    }

    public boolean isCurrent(String source, String version) {
        return version.equals(currentVersion(source));
    }

    public KnowledgeDocumentSnapshot currentSnapshot(String source) {
        String version = currentVersion(source);
        if (version == null) {
            throw new NoSuchElementException("知识资料不存在");
        }
        return registry.findSnapshot(source, version)
                .orElseThrow(() -> new InterviewSessionConflictException(
                        "当前资料没有正文快照，请重新上传原文件后生成题目"));
    }

    public List<MarkdownSearchResult> evidence(String source, String version) {
        if (!isCurrent(source, version)) {
            throw new InterviewSessionConflictException("知识资料版本已变化，请从当前版本重新生成题目");
        }
        KnowledgeDocumentSnapshot snapshot = currentSnapshot(source);
        if (!snapshot.document().version().equals(version)) {
            throw new InterviewSessionConflictException("知识资料版本已变化，请刷新后重试");
        }
        var sections = snapshot.sections().isEmpty()
                ? new MarkdownHeadingParser().parse(snapshot.document()) : snapshot.sections();
        List<MarkdownChunk> chunks = new MarkdownSectionChunker().chunk(sections, maxChunkTokens);
        if (chunks.isEmpty()) {
            throw new InterviewSessionConflictException("资料没有可用于出题的正文片段");
        }
        int count = Math.min(6, chunks.size());
        List<MarkdownSearchResult> results = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int position = count == 1 ? 0 : index * (chunks.size() - 1) / (count - 1);
            MarkdownChunk chunk = chunks.get(position);
            String content = chunk.content();
            if (content.length() > 2000) {
                content = content.substring(0, 2000);
            }
            results.add(new MarkdownSearchResult(content, source, version, chunk.order(),
                    chunk.headingPath(), null, chunk.pageNumber(), chunk.blockType()));
        }
        return List.copyOf(results);
    }
}
