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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfKnowledgeDocumentExtractorTest {

    @Test
    void convertsTextPdfPagesToTraceableMarkdownSections() throws IOException {
        PdfKnowledgeDocumentExtraction extraction = new PdfKnowledgeDocumentExtractor()
                .extract(new MockMultipartFile("file", "collections.pdf", "application/pdf", textPdf(
                        "HashMap structure", "Treeification threshold")));

        assertEquals(2, extraction.pageCount());
        assertEquals("<!-- 第 1 页 -->\n\n# 第 1 页\n\nHashMap structure"
                        + "\n\n<!-- 第 2 页 -->\n\n# 第 2 页\n\nTreeification threshold",
                extraction.markdownContent());
        assertEquals(2, extraction.pages().size());
        assertEquals(1, extraction.pages().get(0).pageNumber());
        assertEquals(0, extraction.pagesNeedingOcr().size());
        assertEquals(0, extraction.warnings().size());
    }

    @Test
    void extractsHeadingsBlocksAndCrossPageChapterPaths() throws IOException {
        PdfKnowledgeDocumentExtractor extractor = new PdfKnowledgeDocumentExtractor();
        PdfKnowledgeDocumentExtraction extraction = extractor.extract(new MockMultipartFile(
                "file", "structured.pdf", "application/pdf", structuredPdf()));

        assertFalse(extraction.markdownContent().contains("JAVA KNOWLEDGE GUIDE"));
        assertTrue(extraction.markdownContent().contains("# 1. HashMap"));
        assertTrue(extraction.markdownContent().contains("## 1.1 Treeification"));

        List<PdfKnowledgeBlock> contentBlocks = extraction.blocks().stream()
                .filter(block -> !block.heading())
                .toList();
        assertEquals(List.of(KnowledgeBlockType.TEXT, KnowledgeBlockType.LIST,
                        KnowledgeBlockType.CODE, KnowledgeBlockType.TABLE,
                        KnowledgeBlockType.TEXT, KnowledgeBlockType.TEXT),
                contentBlocks.stream().map(PdfKnowledgeBlock::type).toList());
        assertEquals(2, contentBlocks.get(4).pageNumber());
        assertEquals(List.of("1. HashMap"), contentBlocks.get(4).headingPath());
        assertEquals(List.of("1. HashMap", "1.1 Treeification"),
                contentBlocks.get(5).headingPath());
        assertTrue(contentBlocks.get(3).content().contains("capacity | 16"));

        List<MarkdownChunk> chunks = new MarkdownSectionChunker().chunk(
                extractor.toSections("java/structured.pdf", extraction.blocks()), 1000);
        assertEquals(contentBlocks.stream().map(PdfKnowledgeBlock::pageNumber).toList(),
                chunks.stream().map(MarkdownChunk::pageNumber).toList());
        assertEquals(contentBlocks.stream().map(PdfKnowledgeBlock::type).toList(),
                chunks.stream().map(MarkdownChunk::blockType).toList());
    }

    @Test
    void keepsAContinuationTableAsTableWhenTheLastPageHasOnlyTwoRows() throws IOException {
        PdfKnowledgeDocumentExtraction extraction = new PdfKnowledgeDocumentExtractor()
                .extract(new MockMultipartFile("file", "continued-table.pdf", "application/pdf",
                        continuedTablePdf()));

        List<PdfKnowledgeBlock> tableBlocks = extraction.blocks().stream()
                .filter(block -> block.type() == KnowledgeBlockType.TABLE)
                .toList();
        assertEquals(2, tableBlocks.size());
        assertEquals(1, tableBlocks.get(0).pageNumber());
        assertEquals(2, tableBlocks.get(1).pageNumber());
        assertTrue(tableBlocks.get(1).content().contains("timeout | 30"));
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

    private byte[] structuredPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font code = new PDType1Font(Standard14Fonts.FontName.COURIER);

            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, firstPage)) {
                drawText(stream, regular, 9, 40, 815, "JAVA KNOWLEDGE GUIDE");
                drawText(stream, bold, 18, 40, 750, "1. HashMap");
                drawText(stream, regular, 11, 40, 720,
                        "HashMap stores entries in buckets and preserves average constant lookup time.");
                drawText(stream, regular, 11, 40, 690, "- Capacity grows when the threshold is reached");
                drawText(stream, code, 10, 40, 660, "if (size > threshold) resize();");
                drawTableRow(stream, regular, 11, 610, "Key", "Value");
                drawTableRow(stream, regular, 11, 590, "capacity", "16");
                drawTableRow(stream, regular, 11, 570, "threshold", "12");
                drawText(stream, regular, 9, 290, 25, "1");
            }

            PDPage secondPage = new PDPage(PDRectangle.A4);
            document.addPage(secondPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, secondPage)) {
                drawText(stream, regular, 9, 40, 815, "JAVA KNOWLEDGE GUIDE");
                drawText(stream, regular, 11, 40, 760,
                        "The next page continues the same HashMap chapter before a new subsection.");
                drawText(stream, bold, 14, 40, 710, "1.1 Treeification");
                drawText(stream, regular, 11, 40, 680,
                        "A long collision chain can become a red black tree after the threshold.");
                drawText(stream, regular, 9, 290, 25, "2");
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] continuedTablePdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, firstPage)) {
                drawText(stream, regular, 18, 40, 750, "1. Configuration");
                drawTableRow(stream, regular, 11, 700, "Key", "Value");
                drawTableRow(stream, regular, 11, 680, "capacity", "16");
                drawTableRow(stream, regular, 11, 660, "threshold", "12");
            }
            PDPage secondPage = new PDPage(PDRectangle.A4);
            document.addPage(secondPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, secondPage)) {
                drawTableRow(stream, regular, 11, 760, "Key", "Value");
                drawTableRow(stream, regular, 11, 740, "timeout", "30");
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void drawTableRow(PDPageContentStream stream, PDType1Font font, float fontSize,
                              float y, String first, String second) throws IOException {
        drawText(stream, font, fontSize, 40, y, first);
        drawText(stream, font, fontSize, 250, y, second);
    }

    private void drawText(PDPageContentStream stream, PDType1Font font, float fontSize,
                          float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }
}
