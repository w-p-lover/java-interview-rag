package com.javainterview.rag.interview.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentAnalyzerTest {

    private final PdfDocumentAnalyzer analyzer = new PdfDocumentAnalyzer();

    @Test
    void reordersClearTwoColumnPageAfterSpanningHeading() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 770, "TWO_COLUMN_RESUME");
                writeLine(stream, 72, 700, "LEFT_SKILLS Java Spring");
                writeLine(stream, 72, 670, "LEFT_EDUCATION Computer Science");
                writeLine(stream, 330, 700, "RIGHT_EXPERIENCE Backend Engineer");
                writeLine(stream, 330, 670, "RIGHT_PROJECT RAG Platform");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.TWO_COLUMN, result.layout());
            assertEquals(PdfPageContentType.TEXT, result.contentType());
            assertOrder(result.content(), "TWO_COLUMN_RESUME", "LEFT_SKILLS", "LEFT_EDUCATION",
                    "RIGHT_EXPERIENCE", "RIGHT_PROJECT");
        }
    }

    @Test
    void recognizesTwoColumnsSeparatedByANarrowPageCenterGutter() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addWidePage(document);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeText(stream, font, 9, 54, 700, paddedLine(font, 9, "LEFT_NARROW_ONE", 235));
                writeText(stream, font, 9, 54, 680, paddedLine(font, 9, "LEFT_NARROW_TWO", 235));
                writeText(stream, font, 9, 317, 700, paddedLine(font, 9, "RIGHT_NARROW_ONE", 120));
                writeText(stream, font, 9, 317, 680, paddedLine(font, 9, "RIGHT_NARROW_TWO", 120));
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.TWO_COLUMN, result.layout());
            assertOrder(result.content(), "LEFT_NARROW_ONE", "LEFT_NARROW_TWO",
                    "RIGHT_NARROW_ONE", "RIGHT_NARROW_TWO");
        }
    }

    @Test
    void keepsSpanningSectionHeadingBetweenTwoColumnRegions() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addWidePage(document);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeColumnRegion(stream, font, 700, "LEFT_BEFORE", "RIGHT_BEFORE");
                writeColumnRegion(stream, font, 680, "LEFT_BEFORE_END", "RIGHT_BEFORE_END");
                writeText(stream, bold, 14, 54, 620,
                        paddedLine(bold, 14, "SPANNING_SECTION", 340));
                writeColumnRegion(stream, font, 560, "LEFT_AFTER", "RIGHT_AFTER");
                writeColumnRegion(stream, font, 540, "LEFT_AFTER_END", "RIGHT_AFTER_END");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.TWO_COLUMN, result.layout());
            assertOrder(result.content(), "LEFT_BEFORE", "LEFT_BEFORE_END",
                    "RIGHT_BEFORE", "RIGHT_BEFORE_END", "SPANNING_SECTION",
                    "LEFT_AFTER", "LEFT_AFTER_END", "RIGHT_AFTER", "RIGHT_AFTER_END");
        }
    }

    @Test
    void keepsSlightlyWideLineInItsLeftColumn() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addWidePage(document);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeColumnRegion(stream, font, 700, "LEFT_START", "RIGHT_START");
                writeText(stream, font, 9, 54, 680,
                        paddedLine(font, 9, "LEFT_SLIGHTLY_WIDE", 250));
                writeText(stream, font, 9, 330, 680,
                        paddedLine(font, 9, "RIGHT_MIDDLE", 130));
                writeColumnRegion(stream, font, 660, "LEFT_END", "RIGHT_END");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.TWO_COLUMN, result.layout());
            assertOrder(result.content(), "LEFT_START", "LEFT_SLIGHTLY_WIDE", "LEFT_END",
                    "RIGHT_START", "RIGHT_MIDDLE", "RIGHT_END");
        }
    }

    @Test
    void keepsCaptionedTableColumnsInSingleColumnLayout() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addWidePage(document);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeText(stream, regular, 9, 54, 770, "4.7 Attributes");
                writeText(stream, regular, 9, 420, 770, "THE CLASS FILE FORMAT");
                writeText(stream, regular, 12, 54, 740,
                        paddedLine(regular, 12, "Table 4.7-C. JVM attributes", 380));
                writeColumnRegion(stream, regular, 700, "ATTRIBUTE_HEADER", "LOCATION_HEADER");
                writeColumnRegion(stream, regular, 680, "ATTRIBUTE_ONE", "LOCATION_ONE");
                writeColumnRegion(stream, regular, 660, "ATTRIBUTE_TWO", "LOCATION_TWO");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.SINGLE_COLUMN, result.layout());
            assertOrder(result.content(), "4.7 Attributes", "THE CLASS FILE FORMAT",
                    "Table 4.7-C", "ATTRIBUTE_HEADER", "LOCATION_HEADER",
                    "ATTRIBUTE_ONE", "LOCATION_ONE", "ATTRIBUTE_TWO", "LOCATION_TWO");
        }
    }

    @Test
    void doesNotTreatTableReferenceAsCaptionBeforeTwoColumnProse() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addWidePage(document);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeText(stream, regular, 12, 54, 740,
                        paddedLine(regular, 12, "Table 1 shows benchmark results", 380));
                writeColumnRegion(stream, regular, 700, "LEFT_RESULT_ONE", "RIGHT_RESULT_ONE");
                writeColumnRegion(stream, regular, 680, "LEFT_RESULT_TWO", "RIGHT_RESULT_TWO");
                writeColumnRegion(stream, regular, 660, "LEFT_RESULT_THREE", "RIGHT_RESULT_THREE");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.TWO_COLUMN, result.layout());
            assertOrder(result.content(), "LEFT_RESULT_ONE", "LEFT_RESULT_TWO", "LEFT_RESULT_THREE",
                    "RIGHT_RESULT_ONE", "RIGHT_RESULT_TWO", "RIGHT_RESULT_THREE");
        }
    }

    @Test
    void keepsSingleColumnPageInVerticalOrder() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 760, "PROFILE");
                writeLine(stream, 72, 720, "Java backend engineer");
                writeLine(stream, 72, 680, "Spring AI and RAG");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.SINGLE_COLUMN, result.layout());
            assertOrder(result.content(), "PROFILE", "Java backend engineer", "Spring AI and RAG");
        }
    }

    @Test
    void doesNotTreatRightAlignedDatesAsSecondColumn() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 740, "Backend Engineer at Example Company");
                writeLine(stream, 470, 740, "2022-2024");
                writeLine(stream, 72, 700, "Java Developer at Sample Company");
                writeLine(stream, 470, 700, "2020-2022");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.SINGLE_COLUMN, result.layout());
            assertOrder(result.content(), "Backend Engineer", "2022-2024", "Java Developer", "2020-2022");
        }
    }

    @Test
    void keepsSlightlyOffsetRightMetadataAfterLeftSubject() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 740, 12, "EXAMPLE UNIVERSITY");
                writeLine(stream, 470, 743.5f, 9, "2021-2025");
                writeLine(stream, 72, 700, 12, "PROJECT ALPHA");
                writeLine(stream, 470, 703.5f, 9, "2024-02 TO 2024-08");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals(PdfPageLayout.SINGLE_COLUMN, result.layout());
            assertOrder(result.content(), "EXAMPLE UNIVERSITY", "2021-2025",
                    "PROJECT ALPHA", "2024-02 TO 2024-08");
        }
    }

    @Test
    void restoresSpaceAcrossAdjacentInlineMonospaceText() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.newLineAtOffset(72, 740);
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.showText("before");
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 12);
                stream.showText("code,");
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.showText("after");
                stream.endText();
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals("before code, after", result.content());
            assertFalse(result.lines().get(0).monospaced());
        }
    }

    @Test
    void keepsLargeWordGapInsideOneColumnAsOneVisualLine() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 740, "left");
                writeLine(stream, 180, 740, "content");
                writeLine(stream, 72, 700, "right content");
            }

            PdfPageAnalysis result = analyzer.analyze(document).get(0);

            assertEquals("left content\nright content", result.content());
        }
    }

    @Test
    void classifiesTextImageAndEmptyPagesIndependently() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage textWithImage = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, textWithImage)) {
                writeLine(stream, 72, 740, "TEXT WITH AVATAR");
                stream.drawImage(image(document), 420, 680, 72, 72);
            }
            PDPage imageOnly = addPage(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, imageOnly)) {
                stream.drawImage(image(document), 72, 500, 300, 200);
            }
            addPage(document);

            List<PdfPageAnalysis> results = analyzer.analyze(document);

            assertEquals(PdfPageContentType.TEXT_WITH_IMAGES, results.get(0).contentType());
            assertEquals(1, results.get(0).imageCount());
            assertEquals(PdfPageContentType.IMAGE_ONLY, results.get(1).contentType());
            assertTrue(results.get(1).requiresOcr());
            assertEquals(PdfPageContentType.EMPTY, results.get(2).contentType());
            assertEquals(false, results.get(2).requiresOcr());
        }
    }

    private PDPage addPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private PDPage addWidePage(PDDocument document) {
        PDPage page = new PDPage(new PDRectangle(612, 792));
        document.addPage(page);
        return page;
    }

    private void writeColumnRegion(PDPageContentStream stream, PDType1Font font, float y,
                                   String leftMarker, String rightMarker) throws IOException {
        writeText(stream, font, 9, 54, y, paddedLine(font, 9, leftMarker, 130));
        writeText(stream, font, 9, 330, y, paddedLine(font, 9, rightMarker, 130));
    }

    private String paddedLine(PDType1Font font, float fontSize, String marker,
                              float minimumWidth) throws IOException {
        StringBuilder text = new StringBuilder(marker);
        while (font.getStringWidth(text.toString()) / 1000.0f * fontSize < minimumWidth) {
            text.append(" x");
        }
        return text.toString();
    }

    private void writeText(PDPageContentStream stream, PDType1Font font, float fontSize,
                           float x, float y, String text) throws IOException {
        stream.setFont(font, fontSize);
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private void writeLine(PDPageContentStream stream, float x, float y, String text) throws IOException {
        writeLine(stream, x, y, 12, text);
    }

    private void writeLine(PDPageContentStream stream, float x, float y,
                           float fontSize, String text) throws IOException {
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private PDImageXObject image(PDDocument document) throws IOException {
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.drawRect(5, 5, image.getWidth() - 10, image.getHeight() - 10);
        graphics.dispose();
        return LosslessFactory.createFromImage(document, image);
    }

    private void assertOrder(String content, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = content.indexOf(marker);
            assertTrue(current > previous, () -> marker + " 顺序错误，实际正文：\n" + content);
            previous = current;
        }
    }
}
