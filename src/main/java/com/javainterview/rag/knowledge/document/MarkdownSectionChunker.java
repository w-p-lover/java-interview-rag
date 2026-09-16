package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.knowledge.token.TokenBudget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownSectionChunker {

    private static final Pattern PARAGRAPH_SEPARATOR = Pattern.compile("\\n(?:[\\t ]*\\n)+");
    private static final Pattern FENCE_START = Pattern.compile("^ {0,3}(`{3,}|~{3,}).*$");
    private static final Pattern TABLE_DELIMITER = Pattern.compile(
            "^ {0,3}\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
    private final TokenBudget tokenBudget;

    public MarkdownSectionChunker() {
        this(new TokenBudget());
    }

    public MarkdownSectionChunker(TokenBudget tokenBudget) {
        this.tokenBudget = Objects.requireNonNull(tokenBudget, "Token 预算不能为空");
    }

    public List<MarkdownChunk> chunk(List<MarkdownSection> sections, int maxTokens) {
        Objects.requireNonNull(sections, "Markdown Section 列表不能为空");
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Chunk 最大 Token 数必须大于 0");
        }

        List<MarkdownChunk> chunks = new ArrayList<>();
        for (MarkdownSection section : sections) {
            Objects.requireNonNull(section, "Markdown Section 不能为空");
            String normalizedContent = section.content().replace("\r\n", "\n").replace('\r', '\n');
            if (normalizedContent.isBlank()) {
                continue;
            }

            if (section.blockType() == KnowledgeBlockType.TABLE) {
                addTableChunks(chunks, section, normalizedContent, maxTokens);
                continue;
            }
            if (section.blockType() != KnowledgeBlockType.TEXT) {
                addBoundedChunks(chunks, section, normalizedContent, maxTokens, section.blockType());
                continue;
            }

            for (ContentBlock block : splitContentBlocks(normalizedContent)) {
                if (block.type() == KnowledgeBlockType.TEXT) {
                    addTextChunks(chunks, section, block.content(), maxTokens);
                } else if (block.type() == KnowledgeBlockType.TABLE) {
                    addTableChunks(chunks, section, block.content(), maxTokens);
                } else {
                    addBoundedChunks(chunks, section, block.content(), maxTokens, block.type());
                }
            }
        }
        return List.copyOf(chunks);
    }

    private void addTextChunks(List<MarkdownChunk> chunks, MarkdownSection section,
                               String content, int maxTokens) {
        StringBuilder currentContent = new StringBuilder();
        for (String paragraph : PARAGRAPH_SEPARATOR.split(content)) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (!tokenBudget.fits(paragraph, maxTokens)) {
                addPendingChunk(chunks, section, currentContent);
                addBoundedChunks(chunks, section, paragraph, maxTokens, KnowledgeBlockType.TEXT);
                continue;
            }

            String combined = currentContent.isEmpty()
                    ? paragraph : currentContent + "\n\n" + paragraph;
            if (!currentContent.isEmpty() && !tokenBudget.fits(combined, maxTokens)) {
                addPendingChunk(chunks, section, currentContent);
            }
            if (!currentContent.isEmpty()) {
                currentContent.append("\n\n");
            }
            currentContent.append(paragraph);
        }
        addPendingChunk(chunks, section, currentContent);
    }

    private void addTableChunks(List<MarkdownChunk> chunks, MarkdownSection section,
                                String content, int maxTokens) {
        String[] lines = content.split("\n", -1);
        int headerLineCount = lines.length > 1 && TABLE_DELIMITER.matcher(lines[1]).matches() ? 2 : 1;
        String header = String.join("\n", Arrays.copyOfRange(lines, 0, headerLineCount));
        if (lines.length <= headerLineCount || !lines[0].contains("|")
                || !tokenBudget.fits(header, maxTokens)) {
            addBoundedChunks(chunks, section, content, maxTokens, KnowledgeBlockType.TABLE);
            return;
        }

        StringBuilder current = new StringBuilder(header);
        for (int index = headerLineCount; index < lines.length; index++) {
            String row = lines[index];
            String candidate = current + "\n" + row;
            if (tokenBudget.fits(candidate, maxTokens)) {
                current.append('\n').append(row);
                continue;
            }
            if (!current.toString().equals(header)) {
                addChunk(chunks, section, current.toString(), KnowledgeBlockType.TABLE);
                current.setLength(0);
                current.append(header);
            }
            if (tokenBudget.fits(header + "\n" + row, maxTokens)) {
                current.append('\n').append(row);
                continue;
            }
            addOversizedTableRow(chunks, section, header, row, maxTokens);
        }
        if (!current.toString().equals(header)) {
            addChunk(chunks, section, current.toString(), KnowledgeBlockType.TABLE);
        }
    }

    private void addOversizedTableRow(List<MarkdownChunk> chunks, MarkdownSection section,
                                      String header, String row, int maxTokens) {
        int start = 0;
        while (start < row.length()) {
            String remaining = row.substring(start);
            String prefix = maximumTableRowPrefix(header, remaining, maxTokens);
            if (prefix.isEmpty()) {
                addBoundedChunks(chunks, section, remaining, maxTokens, KnowledgeBlockType.TABLE);
                return;
            }
            addChunk(chunks, section, header + "\n" + prefix, KnowledgeBlockType.TABLE);
            start += prefix.length();
        }
    }

    private String maximumTableRowPrefix(String header, String row, int maxTokens) {
        int low = 1;
        int high = row.codePointCount(0, row.length());
        int accepted = 0;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int end = row.offsetByCodePoints(0, middle);
            if (tokenBudget.fits(header + "\n" + row.substring(0, end), maxTokens)) {
                accepted = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        if (accepted == 0) {
            return "";
        }
        return row.substring(0, row.offsetByCodePoints(0, accepted));
    }

    private void addBoundedChunks(List<MarkdownChunk> chunks, MarkdownSection section,
                                  String content, int maxTokens, KnowledgeBlockType blockType) {
        int start = 0;
        while (start < content.length()) {
            String remaining = content.substring(start);
            if (tokenBudget.fits(remaining, maxTokens)) {
                addChunk(chunks, section, remaining, blockType);
                return;
            }

            String prefix = tokenBudget.truncate(remaining, maxTokens);
            int limit = start + prefix.length();
            int boundary = findBoundary(content, start, limit, blockType);
            if (boundary <= start) {
                boundary = limit;
            }
            addChunk(chunks, section, content.substring(start, boundary), blockType);
            start = boundary;
        }
    }

    private List<ContentBlock> splitContentBlocks(String content) {
        String[] lines = content.split("\\n", -1);
        List<ContentBlock> blocks = new ArrayList<>();
        List<String> textLines = new ArrayList<>();
        int index = 0;
        while (index < lines.length) {
            Fence fence = openingFence(lines[index]);
            if (fence != null) {
                addContentBlock(blocks, KnowledgeBlockType.TEXT, textLines);
                List<String> codeLines = new ArrayList<>();
                codeLines.add(lines[index++]);
                while (index < lines.length) {
                    String line = lines[index++];
                    codeLines.add(line);
                    if (isClosingFence(line, fence)) {
                        break;
                    }
                }
                addContentBlock(blocks, KnowledgeBlockType.CODE, codeLines);
                continue;
            }
            if (isTableStart(lines, index)) {
                addContentBlock(blocks, KnowledgeBlockType.TEXT, textLines);
                List<String> tableLines = new ArrayList<>();
                tableLines.add(lines[index++]);
                tableLines.add(lines[index++]);
                while (index < lines.length && isTableRow(lines[index])) {
                    tableLines.add(lines[index++]);
                }
                addContentBlock(blocks, KnowledgeBlockType.TABLE, tableLines);
                continue;
            }
            textLines.add(lines[index++]);
        }
        addContentBlock(blocks, KnowledgeBlockType.TEXT, textLines);
        return List.copyOf(blocks);
    }

    private void addContentBlock(List<ContentBlock> blocks, KnowledgeBlockType type, List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) {
            start++;
        }
        while (end > start && lines.get(end - 1).isBlank()) {
            end--;
        }
        if (start < end) {
            blocks.add(new ContentBlock(type, String.join("\n", lines.subList(start, end))));
        }
        lines.clear();
    }

    private Fence openingFence(String line) {
        Matcher matcher = FENCE_START.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        String marker = matcher.group(1);
        return new Fence(marker.charAt(0), marker.length());
    }

    private boolean isClosingFence(String line, Fence fence) {
        String stripped = line.stripLeading();
        if (line.length() - stripped.length() > 3 || stripped.isEmpty()
                || stripped.charAt(0) != fence.marker()) {
            return false;
        }
        int markerLength = 0;
        while (markerLength < stripped.length()
                && stripped.charAt(markerLength) == fence.marker()) {
            markerLength++;
        }
        return markerLength >= fence.length() && stripped.substring(markerLength).isBlank();
    }

    private boolean isTableStart(String[] lines, int index) {
        return index + 1 < lines.length && lines[index].contains("|")
                && TABLE_DELIMITER.matcher(lines[index + 1]).matches();
    }

    private boolean isTableRow(String line) {
        return !line.isBlank() && line.contains("|");
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
        addChunk(chunks, section, currentContent.toString(), KnowledgeBlockType.TEXT);
        currentContent.setLength(0);
    }

    private void addChunk(List<MarkdownChunk> chunks, MarkdownSection section, String content,
                          KnowledgeBlockType blockType) {
        chunks.add(new MarkdownChunk(section.source(), chunks.size(), section.order(),
                section.headingLevel(), section.headingPath(), content,
                section.pageNumber(), blockType));
    }

    private record ContentBlock(KnowledgeBlockType type, String content) {
    }

    private record Fence(char marker, int length) {
    }
}
