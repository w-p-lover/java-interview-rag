package com.javainterview.rag.knowledge.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownHeadingParser {

    private static final Pattern ATX_HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:[\\t ]+|$)(.*)$");
    private static final Pattern CLOSING_MARKERS = Pattern.compile("[\\t ]+#+[\\t ]*$");

    public List<MarkdownSection> parse(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");

        List<MarkdownSection> sections = new ArrayList<>();
        List<Heading> headingStack = new ArrayList<>();
        List<String> bodyLines = new ArrayList<>();
        int currentLevel = 0;
        List<String> currentPath = List.of();
        boolean headingStarted = false;
        Fence activeFence = null;

        String normalizedContent = document.content().replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalizedContent.split("\n", -1)) {
            if (activeFence != null) {
                bodyLines.add(line);
                if (isClosingFence(line, activeFence)) {
                    activeFence = null;
                }
                continue;
            }

            Fence openingFence = findOpeningFence(line);
            if (openingFence != null) {
                bodyLines.add(line);
                activeFence = openingFence;
                continue;
            }

            Matcher headingMatcher = ATX_HEADING.matcher(line);
            if (!headingMatcher.matches()) {
                bodyLines.add(line);
                continue;
            }

            if (headingStarted || containsContent(bodyLines)) {
                sections.add(createSection(document.source(), sections.size(), currentLevel, currentPath, bodyLines));
            }
            bodyLines.clear();

            currentLevel = headingMatcher.group(1).length();
            String title = CLOSING_MARKERS.matcher(headingMatcher.group(2)).replaceFirst("").strip();
            while (!headingStack.isEmpty()
                    && headingStack.get(headingStack.size() - 1).level() >= currentLevel) {
                headingStack.remove(headingStack.size() - 1);
            }
            headingStack.add(new Heading(currentLevel, title));
            currentPath = headingStack.stream().map(Heading::title).toList();
            headingStarted = true;
        }

        if (headingStarted || containsContent(bodyLines)) {
            sections.add(createSection(document.source(), sections.size(), currentLevel, currentPath, bodyLines));
        }
        return List.copyOf(sections);
    }

    private MarkdownSection createSection(String source, int order, int headingLevel,
                                          List<String> headingPath, List<String> bodyLines) {
        int start = 0;
        int end = bodyLines.size();
        while (start < end && bodyLines.get(start).isBlank()) {
            start++;
        }
        while (end > start && bodyLines.get(end - 1).isBlank()) {
            end--;
        }
        String content = String.join("\n", bodyLines.subList(start, end));
        return new MarkdownSection(source, order, headingLevel, headingPath, content);
    }

    private boolean containsContent(List<String> lines) {
        return lines.stream().anyMatch(line -> !line.isBlank());
    }

    private Fence findOpeningFence(String line) {
        int markerStart = markerStart(line);
        if (markerStart < 0 || markerStart >= line.length()) {
            return null;
        }
        char marker = line.charAt(markerStart);
        if (marker != '`' && marker != '~') {
            return null;
        }
        int markerLength = markerLength(line, markerStart, marker);
        return markerLength >= 3 ? new Fence(marker, markerLength) : null;
    }

    private boolean isClosingFence(String line, Fence fence) {
        int markerStart = markerStart(line);
        if (markerStart < 0 || markerStart >= line.length() || line.charAt(markerStart) != fence.marker()) {
            return false;
        }
        int markerLength = markerLength(line, markerStart, fence.marker());
        return markerLength >= fence.length() && line.substring(markerStart + markerLength).isBlank();
    }

    private int markerStart(String line) {
        int index = 0;
        while (index < line.length() && index < 4 && line.charAt(index) == ' ') {
            index++;
        }
        return index <= 3 ? index : -1;
    }

    private int markerLength(String line, int start, char marker) {
        int index = start;
        while (index < line.length() && line.charAt(index) == marker) {
            index++;
        }
        return index - start;
    }

    private record Heading(int level, String title) {
    }

    private record Fence(char marker, int length) {
    }
}
