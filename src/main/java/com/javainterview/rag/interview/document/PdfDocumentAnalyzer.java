package com.javainterview.rag.interview.document;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PdfDocumentAnalyzer {

    private static final float LINE_Y_TOLERANCE = 2.5f;
    private static final float VISUAL_ROW_Y_TOLERANCE = 4.0f;
    private static final float LEFT_COLUMN_LIMIT = 0.48f;
    private static final float RIGHT_COLUMN_LIMIT = 0.52f;
    private static final float MINIMUM_GUTTER_RATIO = 0.06f;
    private static final float MINIMUM_COLUMN_TEXT_WIDTH_RATIO = 0.16f;
    private static final int MINIMUM_LINES_PER_COLUMN = 2;

    public List<PdfPageAnalysis> analyze(PDDocument document) throws IOException {
        PositionCollector collector = new PositionCollector();
        collector.getText(document);

        List<PdfPageAnalysis> pages = new ArrayList<>(document.getNumberOfPages());
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            PDPage page = document.getPage(pageIndex);
            List<TextLine> lines = buildLines(collector.positions(pageIndex + 1));
            float pageWidth = page.getCropBox().getWidth();
            float pageHeight = page.getCropBox().getHeight();
            PageText pageText = orderPage(lines, pageWidth);
            ImageCounter imageCounter = new ImageCounter(page);
            imageCounter.processPage(page);
            int imageCount = imageCounter.imageCount();
            String content = pageText.content().trim();
            int characterCount = content.replaceAll("\\s+", "").length();
            PdfPageContentType contentType = contentType(characterCount, imageCount);
            pages.add(new PdfPageAnalysis(
                    pageIndex + 1,
                    content,
                    characterCount,
                    imageCount,
                    contentType,
                    pageText.layout(),
                    pageWidth,
                    pageHeight,
                    pageText.lines().stream().map(TextLine::toAnalysis).toList()));
        }
        return List.copyOf(pages);
    }

    private List<TextLine> buildLines(List<TextPosition> positions) {
        List<TextPosition> ordered = positions.stream()
                .filter(position -> position.getUnicode() != null && !position.getUnicode().isBlank())
                .sorted(Comparator.comparing(TextPosition::getYDirAdj)
                        .thenComparing(TextPosition::getXDirAdj))
                .toList();
        List<List<TextPosition>> groups = new ArrayList<>();
        for (TextPosition position : ordered) {
            List<TextPosition> line = groups.stream()
                    .filter(candidate -> Math.abs(averageY(candidate) - position.getYDirAdj()) <= LINE_Y_TOLERANCE)
                    .findFirst()
                    .orElseGet(() -> {
                        List<TextPosition> created = new ArrayList<>();
                        groups.add(created);
                        return created;
                    });
            line.add(position);
        }
        List<TextLine> lines = groups.stream()
                .flatMap(group -> toLines(group).stream())
                .filter(line -> !line.text().isBlank())
                .toList();
        return orderByVisualRows(lines);
    }

    private List<TextLine> orderByVisualRows(List<TextLine> lines) {
        List<List<TextLine>> rows = new ArrayList<>();
        for (TextLine line : lines.stream().sorted(Comparator.comparing(TextLine::y)).toList()) {
            List<TextLine> row = rows.stream()
                    .filter(candidate -> Math.abs(averageLineY(candidate) - line.y())
                            <= VISUAL_ROW_Y_TOLERANCE)
                    .findFirst()
                    .orElseGet(() -> {
                        List<TextLine> created = new ArrayList<>();
                        rows.add(created);
                        return created;
                    });
            row.add(line);
        }
        List<TextLine> ordered = new ArrayList<>();
        List<List<TextLine>> sortedRows = rows.stream()
                .sorted(Comparator.comparingDouble(this::averageLineY))
                .toList();
        for (int rowIndex = 0; rowIndex < sortedRows.size(); rowIndex++) {
            List<TextLine> row = sortedRows.get(rowIndex).stream()
                    .sorted(Comparator.comparing(TextLine::xStart))
                    .toList();
            for (TextLine line : row) {
                ordered.add(line.withVisualRow(rowIndex, row.size()));
            }
        }
        return List.copyOf(ordered);
    }

    private double averageLineY(List<TextLine> lines) {
        return lines.stream().mapToDouble(TextLine::y).average().orElse(0.0);
    }

    private float averageY(List<TextPosition> positions) {
        return (float) positions.stream().mapToDouble(TextPosition::getYDirAdj).average().orElse(0.0);
    }

    private List<TextLine> toLines(List<TextPosition> positions) {
        List<TextPosition> ordered = positions.stream()
                .sorted(Comparator.comparing(TextPosition::getXDirAdj))
                .toList();
        List<List<TextPosition>> segments = new ArrayList<>();
        List<TextPosition> current = new ArrayList<>();
        TextPosition previous = null;
        for (TextPosition position : ordered) {
            if (previous != null && isColumnGap(previous, position)) {
                segments.add(current);
                current = new ArrayList<>();
            }
            current.add(position);
            previous = position;
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        return segments.stream().map(this::toLine).toList();
    }

    private boolean isColumnGap(TextPosition previous, TextPosition current) {
        float gap = current.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
        float pageWidth = Math.max(previous.getPageWidth(), current.getPageWidth());
        float spaceWidth = Math.max(previous.getWidthOfSpace(), current.getWidthOfSpace());
        return gap > Math.max(pageWidth * MINIMUM_GUTTER_RATIO, spaceWidth * 4.0f);
    }

    private TextLine toLine(List<TextPosition> ordered) {
        StringBuilder text = new StringBuilder();
        TextPosition previous = null;
        for (TextPosition position : ordered) {
            if (previous != null) {
                float gap = position.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
                float spaceWidth = Math.max(previous.getWidthOfSpace(), position.getWidthOfSpace());
                if (gap > Math.max(1.5f, spaceWidth * 0.45f)
                        || isInlineStyledWordBoundary(previous, position, gap)
                        || isPunctuationWordBoundary(previous, position, gap)) {
                    text.append(' ');
                }
            }
            text.append(position.getUnicode());
            previous = position;
        }
        float xStart = ordered.stream().map(TextPosition::getXDirAdj).min(Float::compare).orElse(0.0f);
        float xEnd = ordered.stream()
                .map(position -> position.getXDirAdj() + position.getWidthDirAdj())
                .max(Float::compare)
                .orElse(xStart);
        float fontSize = (float) ordered.stream()
                .mapToDouble(TextPosition::getFontSizeInPt)
                .average()
                .orElse(0.0);
        boolean bold = ordered.stream().anyMatch(position -> fontName(position).contains("bold"));
        boolean monospaced = ordered.stream().anyMatch(position -> {
            String fontName = fontName(position);
            return fontName.contains("courier") || fontName.contains("mono")
                    || fontName.contains("consolas") || fontName.contains("code");
        });
        return new TextLine(text.toString().trim(), xStart, xEnd, averageY(ordered),
                fontSize, bold, monospaced, 0, 1);
    }

    private boolean isInlineStyledWordBoundary(TextPosition previous, TextPosition current, float gap) {
        if (gap < -0.2f || previous.getUnicode().isBlank() || current.getUnicode().isBlank()
                || !isWordCharacter(previous.getUnicode()) || !isWordCharacter(current.getUnicode())) {
            return false;
        }
        String previousFont = fontFamily(previous);
        String currentFont = fontFamily(current);
        return !previousFont.equals(currentFont)
                && (isMonospacedFont(previousFont) || isMonospacedFont(currentFont));
    }

    private boolean isPunctuationWordBoundary(TextPosition previous, TextPosition current, float gap) {
        return gap >= -0.2f
                && ",;:".contains(previous.getUnicode())
                && isWordCharacter(current.getUnicode())
                && (!isMonospacedFont(fontFamily(previous)) || !isMonospacedFont(fontFamily(current)));
    }

    private boolean isWordCharacter(String value) {
        int codePoint = value.codePointAt(0);
        return Character.isLetterOrDigit(codePoint);
    }

    private String fontFamily(TextPosition position) {
        String name = fontName(position);
        int subsetSeparator = name.indexOf('+');
        return subsetSeparator >= 0 ? name.substring(subsetSeparator + 1) : name;
    }

    private boolean isMonospacedFont(String fontFamily) {
        return fontFamily.contains("courier") || fontFamily.contains("mono")
                || fontFamily.contains("consolas") || fontFamily.contains("code");
    }

    private String fontName(TextPosition position) {
        return position.getFont() == null || position.getFont().getName() == null
                ? "" : position.getFont().getName().toLowerCase(java.util.Locale.ROOT);
    }

    private PageText orderPage(List<TextLine> lines, float pageWidth) {
        if (lines.isEmpty()) {
            return new PageText(List.of(), PdfPageLayout.SINGLE_COLUMN);
        }
        float leftLimit = pageWidth * LEFT_COLUMN_LIMIT;
        float rightLimit = pageWidth * RIGHT_COLUMN_LIMIT;
        List<TextLine> leftCandidates = lines.stream().filter(line -> line.xEnd() <= leftLimit).toList();
        List<TextLine> rightCandidates = lines.stream().filter(line -> line.xStart() >= rightLimit).toList();
        ColumnBlock block = detectColumnBlock(leftCandidates, rightCandidates, pageWidth);
        if (block == null) {
            return new PageText(lines, PdfPageLayout.SINGLE_COLUMN);
        }

        List<TextLine> before = lines.stream().filter(line -> line.y() < block.startY() - LINE_Y_TOLERANCE).toList();
        List<TextLine> left = leftCandidates.stream()
                .filter(line -> inside(line.y(), block.startY(), block.endY()))
                .toList();
        List<TextLine> right = rightCandidates.stream()
                .filter(line -> inside(line.y(), block.startY(), block.endY()))
                .toList();
        List<TextLine> middleSpanning = lines.stream()
                .filter(line -> inside(line.y(), block.startY(), block.endY()))
                .filter(line -> !left.contains(line) && !right.contains(line))
                .toList();
        if (!middleSpanning.isEmpty()) {
            return new PageText(lines, PdfPageLayout.SINGLE_COLUMN);
        }
        List<TextLine> after = lines.stream().filter(line -> line.y() > block.endY() + LINE_Y_TOLERANCE).toList();

        List<TextLine> ordered = new ArrayList<>(before.size() + left.size() + right.size() + after.size());
        ordered.addAll(before);
        ordered.addAll(left);
        ordered.addAll(right);
        ordered.addAll(after);
        return new PageText(List.copyOf(ordered), PdfPageLayout.TWO_COLUMN);
    }

    private ColumnBlock detectColumnBlock(List<TextLine> left, List<TextLine> right, float pageWidth) {
        if (left.size() < MINIMUM_LINES_PER_COLUMN || right.size() < MINIMUM_LINES_PER_COLUMN) {
            return null;
        }
        float startY = Math.max(left.get(0).y(), right.get(0).y());
        float endY = Math.min(left.get(left.size() - 1).y(), right.get(right.size() - 1).y());
        if (endY <= startY) {
            return null;
        }
        List<TextLine> overlappingLeft = left.stream().filter(line -> inside(line.y(), startY, endY)).toList();
        List<TextLine> overlappingRight = right.stream().filter(line -> inside(line.y(), startY, endY)).toList();
        if (overlappingLeft.size() < MINIMUM_LINES_PER_COLUMN
                || overlappingRight.size() < MINIMUM_LINES_PER_COLUMN) {
            return null;
        }
        float leftEdge = overlappingLeft.stream().map(TextLine::xEnd).max(Float::compare).orElse(0.0f);
        float rightEdge = overlappingRight.stream().map(TextLine::xStart).min(Float::compare).orElse(pageWidth);
        if (rightEdge - leftEdge < pageWidth * MINIMUM_GUTTER_RATIO) {
            return null;
        }
        if (averageWidth(overlappingLeft) < pageWidth * MINIMUM_COLUMN_TEXT_WIDTH_RATIO
                || averageWidth(overlappingRight) < pageWidth * MINIMUM_COLUMN_TEXT_WIDTH_RATIO) {
            return null;
        }
        return new ColumnBlock(startY, endY);
    }

    private double averageWidth(List<TextLine> lines) {
        return lines.stream().mapToDouble(line -> line.xEnd() - line.xStart()).average().orElse(0.0);
    }

    private boolean inside(float value, float start, float end) {
        return value >= start - LINE_Y_TOLERANCE && value <= end + LINE_Y_TOLERANCE;
    }

    private String join(List<TextLine> lines) {
        return lines.stream().map(TextLine::text).reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private PdfPageContentType contentType(int characterCount, int imageCount) {
        if (characterCount > 0 && imageCount > 0) {
            return PdfPageContentType.TEXT_WITH_IMAGES;
        }
        if (characterCount > 0) {
            return PdfPageContentType.TEXT;
        }
        if (imageCount > 0) {
            return PdfPageContentType.IMAGE_ONLY;
        }
        return PdfPageContentType.EMPTY;
    }

    private record TextLine(String text, float xStart, float xEnd, float y,
                            float fontSize, boolean bold, boolean monospaced,
                            int visualRow, int visualRowSegmentCount) {

        private TextLine withVisualRow(int row, int segmentCount) {
            return new TextLine(text, xStart, xEnd, y, fontSize, bold, monospaced,
                    row, segmentCount);
        }

        private PdfTextLineAnalysis toAnalysis() {
            return new PdfTextLineAnalysis(text, xStart, xEnd, y, fontSize, bold, monospaced,
                    visualRow, visualRowSegmentCount);
        }
    }

    private record PageText(List<TextLine> lines, PdfPageLayout layout) {

        private String content() {
            return lines.stream().map(TextLine::text)
                    .reduce((left, right) -> left + "\n" + right).orElse("");
        }
    }

    private record ColumnBlock(float startY, float endY) {
    }

    private static final class PositionCollector extends PDFTextStripper {

        private final Map<Integer, List<TextPosition>> positions = new HashMap<>();

        private PositionCollector() throws IOException {
            setSortByPosition(false);
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            positions.computeIfAbsent(getCurrentPageNo(), ignored -> new ArrayList<>()).add(text);
            super.processTextPosition(text);
        }

        private List<TextPosition> positions(int pageNumber) {
            return positions.getOrDefault(pageNumber, List.of());
        }
    }

    private static final class ImageCounter extends PDFGraphicsStreamEngine {

        private int imageCount;
        private Point2D currentPoint;

        private ImageCounter(PDPage page) {
            super(page);
        }

        @Override
        public void drawImage(PDImage pdImage) {
            imageCount++;
        }

        private int imageCount() {
            return imageCount;
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
            currentPoint = new Point2D.Float(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            currentPoint = new Point2D.Float(x, y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            currentPoint = new Point2D.Float(x3, y3);
        }

        @Override
        public Point2D getCurrentPoint() {
            return currentPoint;
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(org.apache.pdfbox.cos.COSName shadingName) {
        }
    }
}
