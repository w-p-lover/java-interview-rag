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

            StringBuilder currentContent = new StringBuilder();
            for (String paragraph : PARAGRAPH_SEPARATOR.split(normalizedContent)) {
                if (paragraph.isBlank()) {
                    continue;
                }
                if (paragraph.length() > maxLength) {
                    addPendingChunk(chunks, section, currentContent);
                    for (int start = 0; start < paragraph.length(); start += maxLength) {
                        int end = Math.min(start + maxLength, paragraph.length());
                        addChunk(chunks, section, paragraph.substring(start, end));
                    }
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
                section.headingLevel(), section.headingPath(), content));
    }
}
