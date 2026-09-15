package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.interview.document.PdfPageAnalysis;
import com.javainterview.rag.interview.document.PdfPageLayout;
import com.javainterview.rag.interview.document.PdfTextLineAnalysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 PDF 行级版面证据转换为可追踪的章节与内容块。
 */
public final class PdfKnowledgeStructureParser {

    private static final Pattern LIST_PREFIX = Pattern.compile(
            "^(?:[-*•●▪◦]|\\d+[.)、]|[a-zA-Z][.)])\\s+.+$");
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(\\d+(?:\\.\\d+){0,5})(?:[.、．]|\\s)+.+$");
    private static final Pattern PAGE_NUMBER = Pattern.compile(
            "^(?:page\\s*)?[-—–]?\\s*\\d+\\s*[-—–]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_CAPTION = Pattern.compile(
            "^(?:table\\s+[\\p{Alnum}]+(?:[.\\-][\\p{Alnum}]+)*(?:\\s+\\(cont\\.\\))?[.:]"
                    + "|表\\s*[0-9]+(?:[.\\-][0-9]+)*[：:.])",
            Pattern.CASE_INSENSITIVE);
    private static final int MIN_TABLE_ROWS = 3;

    public List<PdfKnowledgeBlock> parse(List<PdfPageAnalysis> pages) {
        Objects.requireNonNull(pages, "PDF 页面不能为空");
        Set<String> repeatedMargins = repeatedMarginTexts(pages);
        float bodyFontSize = bodyFontSize(pages, repeatedMargins);
        List<Float> headingFontSizes = headingFontSizes(pages, repeatedMargins, bodyFontSize);

        List<PdfKnowledgeBlock> blocks = new ArrayList<>();
        List<Heading> headingStack = new ArrayList<>();
        for (PdfPageAnalysis page : pages) {
            List<PdfTextLineAnalysis> lines = page.lines().stream()
                    .filter(line -> !ignoreLine(page, line, repeatedMargins))
                    .toList();
            if (lines.isEmpty()) {
                continue;
            }
            boolean continuingTable = continuesTable(blocks, page.pageNumber(), headingStack);
            Set<Integer> tableRows = tableRows(page, lines, continuingTable);
            List<StructuredLine> structuredLines = combineTableRows(lines, tableRows);
            boolean pageHasHeading = structuredLines.stream()
                    .anyMatch(line -> headingLevel(line, bodyFontSize, headingFontSizes) > 0);
            if (!pageHasHeading && (headingStack.isEmpty() || syntheticOnly(headingStack))) {
                headingStack.clear();
                headingStack.add(new Heading(1, "第 " + page.pageNumber() + " 页", true));
                blocks.add(new PdfKnowledgeBlock(page.pageNumber(), KnowledgeBlockType.TEXT, 1,
                        headingStack.stream().map(Heading::title).toList(),
                        headingStack.get(0).title()));
            }
            appendPageBlocks(blocks, headingStack, page.pageNumber(), structuredLines,
                    bodyFontSize, headingFontSizes);
        }
        return List.copyOf(blocks);
    }

    public List<MarkdownSection> toSections(String source, List<PdfKnowledgeBlock> blocks) {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(blocks, "PDF 内容块不能为空");
        List<MarkdownSection> sections = new ArrayList<>(blocks.size());
        int currentHeadingLevel = 0;
        for (PdfKnowledgeBlock block : blocks) {
            if (block.heading()) {
                currentHeadingLevel = block.headingLevel();
            }
            sections.add(new MarkdownSection(source, sections.size(), currentHeadingLevel,
                    block.headingPath(), block.heading() ? "" : block.content(),
                    block.pageNumber(), block.type()));
        }
        return List.copyOf(sections);
    }

    public String toMarkdown(List<PdfKnowledgeBlock> blocks) {
        Objects.requireNonNull(blocks, "PDF 内容块不能为空");
        StringBuilder markdown = new StringBuilder();
        int currentPage = -1;
        for (PdfKnowledgeBlock block : blocks) {
            if (block.pageNumber() != currentPage) {
                appendSeparated(markdown, "<!-- 第 " + block.pageNumber() + " 页 -->");
                currentPage = block.pageNumber();
            }
            if (block.heading()) {
                appendSeparated(markdown, "#".repeat(block.headingLevel()) + " " + block.content());
            } else if (block.type() == KnowledgeBlockType.CODE) {
                appendSeparated(markdown, "```text\n" + block.content() + "\n```");
            } else {
                appendSeparated(markdown, block.content());
            }
        }
        return markdown.toString().trim();
    }

    private void appendPageBlocks(List<PdfKnowledgeBlock> blocks, List<Heading> headingStack,
                                  int pageNumber, List<StructuredLine> lines,
                                  float bodyFontSize, List<Float> headingFontSizes) {
        KnowledgeBlockType pendingType = null;
        List<String> pendingLines = new ArrayList<>();
        for (StructuredLine line : lines) {
            int level = headingLevel(line, bodyFontSize, headingFontSizes);
            if (level > 0) {
                flush(blocks, headingStack, pageNumber, pendingType, pendingLines);
                while (!headingStack.isEmpty()
                        && headingStack.get(headingStack.size() - 1).level() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(new Heading(level, line.text(), false));
                blocks.add(new PdfKnowledgeBlock(pageNumber, KnowledgeBlockType.TEXT, level,
                        headingStack.stream().map(Heading::title).toList(), line.text()));
                pendingType = null;
                continue;
            }

            KnowledgeBlockType type = blockType(line);
            if (pendingType != null && pendingType != type) {
                flush(blocks, headingStack, pageNumber, pendingType, pendingLines);
            }
            pendingType = type;
            pendingLines.add(line.text());
        }
        flush(blocks, headingStack, pageNumber, pendingType, pendingLines);
    }

    private boolean syntheticOnly(List<Heading> headingStack) {
        return headingStack.stream().allMatch(Heading::synthetic);
    }

    private void flush(List<PdfKnowledgeBlock> blocks, List<Heading> headingStack,
                       int pageNumber, KnowledgeBlockType type, List<String> lines) {
        if (type == null || lines.isEmpty()) {
            return;
        }
        List<String> headingPath = headingStack.isEmpty()
                ? List.of("第 " + pageNumber + " 页")
                : headingStack.stream().map(Heading::title).toList();
        blocks.add(new PdfKnowledgeBlock(pageNumber, type, 0, headingPath,
                String.join("\n", lines)));
        lines.clear();
    }

    private KnowledgeBlockType blockType(StructuredLine line) {
        if (line.table()) {
            return KnowledgeBlockType.TABLE;
        }
        if (line.monospaced()) {
            return KnowledgeBlockType.CODE;
        }
        if (LIST_PREFIX.matcher(line.text()).matches()) {
            return KnowledgeBlockType.LIST;
        }
        return KnowledgeBlockType.TEXT;
    }

    private int headingLevel(StructuredLine line, float bodyFontSize, List<Float> headingFontSizes) {
        if (line.table() || line.monospaced() || line.text().length() > 120) {
            return 0;
        }
        Matcher numbered = NUMBERED_HEADING.matcher(line.text());
        boolean emphasized = line.fontSize() >= Math.max(bodyFontSize + 1.5f, bodyFontSize * 1.15f)
                || (line.bold() && line.fontSize() >= bodyFontSize);
        if (!emphasized) {
            return 0;
        }
        if (numbered.matches()) {
            return Math.min(6, 1 + (int) numbered.group(1).chars().filter(value -> value == '.').count());
        }
        for (int index = 0; index < headingFontSizes.size(); index++) {
            if (Math.abs(headingFontSizes.get(index) - line.fontSize()) < 0.2f) {
                return Math.min(6, index + 1);
            }
        }
        return 0;
    }

    private List<Float> headingFontSizes(List<PdfPageAnalysis> pages, Set<String> repeatedMargins,
                                         float bodyFontSize) {
        return pages.stream()
                .flatMap(page -> page.lines().stream()
                        .filter(line -> !ignoreLine(page, line, repeatedMargins)))
                .filter(line -> !line.monospaced())
                .filter(line -> line.text().length() <= 120)
                .filter(line -> line.fontSize() >= Math.max(bodyFontSize + 1.5f, bodyFontSize * 1.15f)
                        || (line.bold() && line.fontSize() >= bodyFontSize))
                .map(PdfTextLineAnalysis::fontSize)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(6)
                .toList();
    }

    private float bodyFontSize(List<PdfPageAnalysis> pages, Set<String> repeatedMargins) {
        List<Float> sizes = pages.stream()
                .flatMap(page -> page.lines().stream()
                        .filter(line -> !ignoreLine(page, line, repeatedMargins)))
                .filter(line -> !line.monospaced())
                .filter(line -> line.text().length() >= 4)
                .map(PdfTextLineAnalysis::fontSize)
                .filter(size -> size > 0)
                .sorted()
                .toList();
        if (sizes.isEmpty()) {
            return 12.0f;
        }
        return sizes.get(sizes.size() / 2);
    }

    private Set<Integer> tableRows(PdfPageAnalysis page, List<PdfTextLineAnalysis> lines,
                                   boolean continuingTable) {
        if (page.layout() == PdfPageLayout.TWO_COLUMN) {
            return Set.of();
        }
        Map<Integer, List<PdfTextLineAnalysis>> rows = lines.stream()
                .collect(java.util.stream.Collectors.groupingBy(PdfTextLineAnalysis::visualRow,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<Integer> candidates = rows.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .toList();
        if (candidates.size() >= MIN_TABLE_ROWS) {
            return captionedTableRows(lines, rows, candidates);
        }
        if (!continuingTable || candidates.size() < 2) {
            return Set.of();
        }
        int firstCandidate = candidates.stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
        int lastCandidate = candidates.stream().mapToInt(Integer::intValue).max().orElse(Integer.MIN_VALUE);
        boolean startsNearPageTop = firstCandidate <= lines.get(0).visualRow() + 1;
        boolean contiguous = lastCandidate - firstCandidate + 1 == candidates.size();
        return startsNearPageTop && contiguous ? Set.copyOf(candidates) : Set.of();
    }

    private Set<Integer> captionedTableRows(List<PdfTextLineAnalysis> lines,
                                            Map<Integer, List<PdfTextLineAnalysis>> rows,
                                            List<Integer> candidates) {
        int firstCandidate = candidates.stream().mapToInt(Integer::intValue).min().orElseThrow();
        boolean captionBeforeTable = lines.stream()
                .filter(line -> line.visualRow() < firstCandidate)
                .anyMatch(line -> TABLE_CAPTION.matcher(line.text()).find());
        if (!captionBeforeTable) {
            return Set.copyOf(candidates);
        }
        int lastTableRow = candidates.stream().mapToInt(Integer::intValue).max().orElseThrow();
        for (Map.Entry<Integer, List<PdfTextLineAnalysis>> entry : rows.entrySet()) {
            if (entry.getKey() <= lastTableRow) {
                continue;
            }
            if (entry.getValue().stream().allMatch(PdfTextLineAnalysis::monospaced)) {
                lastTableRow = entry.getKey();
            } else {
                break;
            }
        }
        Set<Integer> tableRows = new HashSet<>();
        for (Integer row : rows.keySet()) {
            if (row >= firstCandidate && row <= lastTableRow) {
                tableRows.add(row);
            }
        }
        return Set.copyOf(tableRows);
    }

    private boolean continuesTable(List<PdfKnowledgeBlock> blocks, int pageNumber,
                                   List<Heading> headingStack) {
        if (blocks.isEmpty()) {
            return false;
        }
        PdfKnowledgeBlock previous = blocks.get(blocks.size() - 1);
        return previous.type() == KnowledgeBlockType.TABLE
                && previous.pageNumber() == pageNumber - 1
                && previous.headingPath().equals(headingStack.stream().map(Heading::title).toList());
    }

    private List<StructuredLine> combineTableRows(List<PdfTextLineAnalysis> lines, Set<Integer> tableRows) {
        Map<Integer, List<PdfTextLineAnalysis>> rows = new LinkedHashMap<>();
        for (PdfTextLineAnalysis line : lines) {
            rows.computeIfAbsent(line.visualRow(), ignored -> new ArrayList<>()).add(line);
        }
        List<StructuredLine> result = new ArrayList<>();
        Set<Integer> emittedTableRows = new HashSet<>();
        for (PdfTextLineAnalysis line : lines) {
            if (tableRows.contains(line.visualRow())) {
                if (emittedTableRows.add(line.visualRow())) {
                    List<PdfTextLineAnalysis> row = rows.get(line.visualRow());
                    result.add(new StructuredLine(row.stream().map(PdfTextLineAnalysis::text)
                            .reduce((left, right) -> left + " | " + right).orElse(""),
                            averageFontSize(row), row.stream().anyMatch(PdfTextLineAnalysis::bold),
                            false, true));
                }
            } else {
                result.add(new StructuredLine(line.text(), line.fontSize(), line.bold(),
                        line.monospaced(), false));
            }
        }
        return List.copyOf(result);
    }

    private float averageFontSize(List<PdfTextLineAnalysis> lines) {
        return (float) lines.stream().mapToDouble(PdfTextLineAnalysis::fontSize).average().orElse(0.0);
    }

    private Set<String> repeatedMarginTexts(List<PdfPageAnalysis> pages) {
        Map<String, Set<Integer>> occurrences = new HashMap<>();
        for (PdfPageAnalysis page : pages) {
            for (PdfTextLineAnalysis line : page.lines()) {
                if (marginLine(page, line)) {
                    occurrences.computeIfAbsent(normalize(line.text()), ignored -> new HashSet<>())
                            .add(page.pageNumber());
                }
            }
        }
        return occurrences.entrySet().stream()
                .filter(entry -> !entry.getKey().isBlank() && entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean ignoreLine(PdfPageAnalysis page, PdfTextLineAnalysis line,
                               Set<String> repeatedMargins) {
        return line.text().isBlank()
                || (marginLine(page, line) && !line.monospaced()
                && repeatedMargins.contains(normalize(line.text())))
                || (marginLine(page, line) && PAGE_NUMBER.matcher(line.text().strip()).matches());
    }

    private boolean marginLine(PdfPageAnalysis page, PdfTextLineAnalysis line) {
        return line.y() <= page.height() * 0.10f || line.y() >= page.height() * 0.90f;
    }

    private String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void appendSeparated(StringBuilder target, String value) {
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append(value);
    }

    private record Heading(int level, String title, boolean synthetic) {
    }

    private record StructuredLine(String text, float fontSize, boolean bold,
                                  boolean monospaced, boolean table) {
    }
}
