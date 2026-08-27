package com.javainterview.rag.knowledge.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfKnowledgeDocumentExtractorTest {

    @Test
    void convertsTextPdfPagesToTraceableMarkdownSections() throws IOException {
        PdfKnowledgeDocumentExtraction extraction = new PdfKnowledgeDocumentExtractor()
                .extract(new MockMultipartFile("file", "collections.pdf", "application/pdf", textPdf(
                        "HashMap structure", "Treeification threshold")));

        assertEquals(2, extraction.pageCount());
        assertEquals("# 第 1 页\n\nHashMap structure\n\n# 第 2 页\n\nTreeification threshold",
                extraction.markdownContent());
        assertEquals(2, extraction.pages().size());
        assertEquals(1, extraction.pages().get(0).pageNumber());
        assertEquals(0, extraction.pagesNeedingOcr().size());
        assertEquals(0, extraction.warnings().size());
    }

    @Test
    void rejectsPdfWithoutTextInsteadOfIndexingAnEmptyDocument() throws IOException {
        assertEquals("PDF 未提取到可用文字，可能是扫描件；当前暂不支持 OCR",
                assertThrows(IllegalArgumentException.class, () -> new PdfKnowledgeDocumentExtractor()
                        .extract(new MockMultipartFile("file", "scan.pdf", "application/pdf", emptyPdf())))
                        .getMessage());
    }

    @Test
    void rejectsNonPdfFilesAtThePdfBoundary() {
        assertEquals("当前仅支持 PDF 文件",
                assertThrows(IllegalArgumentException.class, () -> new PdfKnowledgeDocumentExtractor()
                        .extract(new MockMultipartFile("file", "notes.md", "text/markdown", "# Java".getBytes())))
                        .getMessage());
    }

    private byte[] textPdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String content : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(40, 760);
                    stream.showText(content);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] emptyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(output);
            return output.toByteArray();
        }
    }
}
