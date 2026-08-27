package com.javainterview.rag.knowledge.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class KnowledgeDocumentOutlineBuilder {

    public KnowledgeDocumentOutline build(List<MarkdownSection> sections, List<MarkdownChunk> chunks) {
        Objects.requireNonNull(sections, "Markdown Section 列表不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");

        if (sections.stream().anyMatch(section -> section.pageNumber() != null)) {
            return buildPdfOutline(sections, chunks);
        }

        Map<Integer, List<MarkdownChunk>> chunksBySection = chunks.stream()
                .collect(Collectors.groupingBy(MarkdownChunk::sectionOrder));
        List<MutableNode> roots = new ArrayList<>();
        List<MutableNode> stack = new ArrayList<>();
        int headingCount = 0;

        for (MarkdownSection section : sections) {
            if (section.headingLevel() == 0 || section.headingPath().isEmpty()) {
                continue;
            }
            List<MarkdownChunk> directChunks = chunksBySection.getOrDefault(section.order(), List.of());
            MutableNode node = new MutableNode(
                    section.headingPath().get(section.headingPath().size() - 1),
                    section.headingLevel(),
                    directChunks.isEmpty() ? null : directChunks.get(0).order(),
                    directChunks.size());
            while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= node.level) {
                stack.remove(stack.size() - 1);
            }
            if (stack.isEmpty()) {
                roots.add(node);
            } else {
                stack.get(stack.size() - 1).children.add(node);
            }
            stack.add(node);
            headingCount++;
        }

        return new KnowledgeDocumentOutline(
                headingCount,
                chunks.size(),
                roots.stream().map(MutableNode::toImmutable).toList());
    }

    private KnowledgeDocumentOutline buildPdfOutline(List<MarkdownSection> sections,
                                                      List<MarkdownChunk> chunks) {
        Map<Integer, List<MarkdownChunk>> chunksBySection = chunks.stream()
                .collect(Collectors.groupingBy(MarkdownChunk::sectionOrder));
        Map<List<String>, MutableNode> nodesByPath = new java.util.LinkedHashMap<>();
        List<MutableNode> roots = new ArrayList<>();
        List<MutableNode> stack = new ArrayList<>();

        for (MarkdownSection section : sections) {
            if (section.headingPath().isEmpty()) {
                continue;
            }
            List<String> path = section.headingPath();
            MutableNode node;
            if (section.content().isBlank()) {
                node = new MutableNode(path.get(path.size() - 1), section.headingLevel(), null, 0);
                while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= node.level) {
                    stack.remove(stack.size() - 1);
                }
                if (stack.isEmpty()) {
                    roots.add(node);
                } else {
                    stack.get(stack.size() - 1).children.add(node);
                }
                stack.add(node);
                nodesByPath.put(path, node);
            } else {
                node = nodesByPath.get(path);
            }
            if (node != null) {
                node.addDirectChunks(chunksBySection.getOrDefault(section.order(), List.of()));
            }
        }

        return new KnowledgeDocumentOutline(nodesByPath.size(), chunks.size(),
                roots.stream().map(MutableNode::toImmutable).toList());
    }

    private static final class MutableNode {
        private final String title;
        private final int level;
        private Integer firstChunkOrder;
        private int directChunkCount;
        private final List<MutableNode> children = new ArrayList<>();

        private MutableNode(String title, int level, Integer firstChunkOrder, int directChunkCount) {
            this.title = title;
            this.level = level;
            this.firstChunkOrder = firstChunkOrder;
            this.directChunkCount = directChunkCount;
        }

        private void addDirectChunks(List<MarkdownChunk> chunks) {
            if (chunks.isEmpty()) {
                return;
            }
            if (firstChunkOrder == null) {
                firstChunkOrder = chunks.get(0).order();
            }
            directChunkCount += chunks.size();
        }

        private KnowledgeDocumentOutlineNode toImmutable() {
            return new KnowledgeDocumentOutlineNode(
                    title, level, firstChunkOrder, directChunkCount,
                    children.stream().map(MutableNode::toImmutable).toList());
        }
    }
}
