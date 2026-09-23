package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSnapshot;
import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownHeadingParser;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownSectionChunker;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankSources {
    private static final Pattern ASCII_TERM = Pattern.compile("[a-z][a-z0-9_.-]{2,}");
    private static final Pattern HAN_SEQUENCE = Pattern.compile("\\p{IsHan}{2,}");

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
        List<MarkdownChunk> chunks = chunks(source, version);
        int count = Math.min(6, chunks.size());
        List<MarkdownSearchResult> results = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int position = count == 1 ? 0 : index * (chunks.size() - 1) / (count - 1);
            results.add(toResult(chunks.get(position), source, version));
        }
        return List.copyOf(results);
    }

    public List<MarkdownSearchResult> relevantEvidence(String source, String version,
                                                       String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("题目证据查询不能为空");
        }
        if (limit < 1 || limit > 6) {
            throw new IllegalArgumentException("题目证据数量必须在 1 到 6 之间");
        }
        Set<String> terms = searchTerms(query);
        return chunks(source, version).stream()
                .sorted(Comparator.comparingInt((MarkdownChunk chunk) -> relevanceScore(chunk, terms))
                        .reversed()
                        .thenComparingInt(MarkdownChunk::order))
                .limit(limit)
                .map(chunk -> toResult(chunk, source, version))
                .toList();
    }

    private List<MarkdownChunk> chunks(String source, String version) {
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
        return chunks;
    }

    private int relevanceScore(MarkdownChunk chunk, Set<String> terms) {
        String text = normalize(String.join(" ", chunk.headingPath()) + " " + chunk.content());
        int score = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                score += isAscii(term) ? 4 : 1;
            }
        }
        return score;
    }

    private Set<String> searchTerms(String query) {
        String normalized = normalize(query);
        Set<String> terms = new HashSet<>();
        Matcher ascii = ASCII_TERM.matcher(normalized);
        while (ascii.find()) {
            terms.add(ascii.group());
        }
        Matcher han = HAN_SEQUENCE.matcher(normalized);
        while (han.find()) {
            String sequence = han.group();
            for (int index = 0; index < sequence.length() - 1; index++) {
                terms.add(sequence.substring(index, index + 2));
            }
        }
        return terms;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private boolean isAscii(String value) {
        return value.charAt(0) < 128;
    }

    private MarkdownSearchResult toResult(MarkdownChunk chunk, String source, String version) {
        String content = chunk.content();
        if (content.length() > 2000) {
            content = content.substring(0, 2000);
        }
        return new MarkdownSearchResult(content, source, version, chunk.order(),
                chunk.headingPath(), null, chunk.pageNumber(), chunk.blockType());
    }
}
