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
