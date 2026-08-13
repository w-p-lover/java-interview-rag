package com.javainterview.rag.interview.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfInterviewDocumentTextExtractorTest {

    private final PdfInterviewDocumentTextExtractor extractor = new PdfInterviewDocumentTextExtractor();

    @Test
    void extractsTextFromEachPdfPageInPositionOrder() throws IOException {
        InterviewDocumentExtraction extraction = extractor.extract(file("resume.pdf", textPdf(
                "Java backend experience", "RAG project experience")));

        assertEquals("PDF", extraction.format());
        assertEquals(2, extraction.pageCount());
        assertEquals(PdfDocumentType.TEXT_BASED, extraction.documentType());
        assertEquals(2, extraction.textPageCount());
        assertEquals(0, extraction.imagePageCount());
        assertEquals(0, extraction.reorderedPageCount());
        assertEquals(true, extraction.pagesNeedingOcr().isEmpty());
        assertEquals(true, extraction.content().contains("Java backend experience"));
        assertEquals(true, extraction.content().contains("RAG project experience"));
    }

    @Test
    void returnsTextFromMixedPdfAndIdentifiesPagesNeedingOcr() throws IOException {
        InterviewDocumentExtraction extraction = extractor.extract(file("mixed-resume.pdf", mixedPdf()));

        assertEquals(PdfDocumentType.MIXED, extraction.documentType());
        assertEquals(2, extraction.pageCount());
        assertEquals(1, extraction.textPageCount());
        assertEquals(1, extraction.imagePageCount());
        assertEquals(java.util.List.of(2), extraction.pagesNeedingOcr());
        assertEquals(PdfPageContentType.TEXT, extraction.pages().get(0).contentType());
        assertEquals(PdfPageContentType.IMAGE_ONLY, extraction.pages().get(1).contentType());
        assertEquals(true, extraction.warnings().get(0).contains("第 2 页"));
    }

    @Test
    void rejectsEmptyUnsupportedAndOversizedFiles() {
        assertEquals("PDF 文件不能为空", assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(file("resume.pdf", new byte[0]))).getMessage());
        assertEquals("当前仅支持 PDF 文件", assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(file("resume.txt", "resume".getBytes()))).getMessage());
        assertEquals("PDF 文件不能超过 10 MiB", assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(file("resume.pdf", new byte[10 * 1024 * 1024 + 1]))).getMessage());
    }

    @Test
    void reportsTextlessPdfAsOcrNotSupported() throws IOException {
        assertEquals("未提取到可用文字，可能是扫描件；当前暂不支持 OCR", assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(file("scanned-resume.pdf", blankPdf()))).getMessage());
    }

    @Test
    void reportsInvalidPdfContent() {
        assertEquals("PDF 解析失败，请确认文件完整", assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(file("broken.pdf", "not a pdf".getBytes()))).getMessage());
    }

    private MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/pdf", content);
    }

    private byte[] textPdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.beginText();
                    stream.newLineAtOffset(72, 720);
                    stream.showText(pageText);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] blankPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] mixedPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage textPage = new PDPage();
            document.addPage(textPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, textPage)) {
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.beginText();
                stream.newLineAtOffset(72, 720);
                stream.showText("Java backend experience");
                stream.endText();
            }
            PDPage imagePage = new PDPage();
            document.addPage(imagePage);
            BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("scanned page", 30, 60);
            graphics.dispose();
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream stream = new PDPageContentStream(document, imagePage)) {
                stream.drawImage(pdfImage, 72, 400, 300, 200);
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
