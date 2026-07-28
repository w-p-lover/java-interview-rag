package com.javainterview.rag.interview.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

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
        assertEquals(true, extraction.content().contains("Java backend experience"));
        assertEquals(true, extraction.content().contains("RAG project experience"));
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
}
