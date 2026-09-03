package com.javainterview.rag.knowledge.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MarkdownSectionChunker {

    private static final Pattern PARAGRAPH_SEPARATOR = Pattern.compile("\\n(?:[\\t ]*\\n)+");

    public List<MarkdownChunk> chunk(List<MarkdownSection> sections, int maxLength) {
        Objects.requireNonNull(sections, "Markdown Section 列表不能为空");
        if (maxLength <= 0) {
            throw new IllegalArgumentException("Chunk 最大长度必须大于 0");
        }

        List<MarkdownChunk> chunks = new ArrayList<>();
        for (MarkdownSection section : sections) {
            Objects.requireNonNull(section, "Markdown Section 不能为空");
            String normalizedContent = section.content().replace("\r\n", "\n").replace('\r', '\n');
            if (normalizedContent.isBlank()) {
                continue;
            }

            if (section.blockType() != KnowledgeBlockType.TEXT) {
                addBoundedChunks(chunks, section, normalizedContent, maxLength);
                continue;
            }

            StringBuilder currentContent = new StringBuilder();
            for (String paragraph : PARAGRAPH_SEPARATOR.split(normalizedContent)) {
                if (paragraph.isBlank()) {
                    continue;
                }
                if (paragraph.length() > maxLength) {
                    addPendingChunk(chunks, section, currentContent);
                    addBoundedChunks(chunks, section, paragraph, maxLength);
                    continue;
                }

                int combinedLength = currentContent.length() + (currentContent.isEmpty() ? 0 : 2)
                        + paragraph.length();
                if (!currentContent.isEmpty() && combinedLength > maxLength) {
                    addPendingChunk(chunks, section, currentContent);
                }
                if (!currentContent.isEmpty()) {
                    currentContent.append("\n\n");
                }
                currentContent.append(paragraph);
            }
            addPendingChunk(chunks, section, currentContent);
        }
        return List.copyOf(chunks);
    }

    private void addBoundedChunks(List<MarkdownChunk> chunks, MarkdownSection section,
                                  String content, int maxLength) {
        int start = 0;
        while (start < content.length()) {
            int remaining = content.length() - start;
            if (remaining <= maxLength) {
                addChunk(chunks, section, content.substring(start));
                return;
            }

            int limit = start + maxLength;
            int boundary = findBoundary(content, start, limit, section.blockType());
            if (boundary <= start) {
                boundary = limit;
            }
            addChunk(chunks, section, content.substring(start, boundary));
            start = boundary;
        }
    }

    private int findBoundary(String content, int start, int limit, KnowledgeBlockType blockType) {
        int lineBoundary = -1;
        int whitespaceBoundary = -1;
        int sentenceBoundary = -1;
        for (int index = start + 1; index <= limit; index++) {
            char current = content.charAt(index - 1);
            if (current == '\n') {
                lineBoundary = index;
                continue;
            }
            if (Character.isWhitespace(current)) {
                whitespaceBoundary = index;
                continue;
            }
            if (blockType == KnowledgeBlockType.TEXT && isSentenceEnd(content, index - 1)) {
                sentenceBoundary = index;
            }
        }
        if (blockType != KnowledgeBlockType.TEXT) {
            return lineBoundary >= 0 ? lineBoundary : whitespaceBoundary;
        }
        return sentenceBoundary >= 0 ? sentenceBoundary : whitespaceBoundary;
    }

    private boolean isSentenceEnd(String content, int index) {
        char current = content.charAt(index);
        if ("。！？!?；;".indexOf(current) >= 0) {
            return true;
        }
        if (current != '.') {
            return false;
        }
        boolean previousDigit = index > 0 && Character.isDigit(content.charAt(index - 1));
        boolean nextDigit = index + 1 < content.length() && Character.isDigit(content.charAt(index + 1));
        return !(previousDigit && nextDigit);
    }

    private void addPendingChunk(List<MarkdownChunk> chunks, MarkdownSection section,
                                 StringBuilder currentContent) {
        if (currentContent.isEmpty()) {
            return;
        }
        addChunk(chunks, section, currentContent.toString());
        currentContent.setLength(0);
    }

    private void addChunk(List<MarkdownChunk> chunks, MarkdownSection section, String content) {
        chunks.add(new MarkdownChunk(section.source(), chunks.size(), section.order(),
                section.headingLevel(), section.headingPath(), content,
                section.pageNumber(), section.blockType()));
    }
}
