package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredRagEvaluationCorpusTest {

    @Test
    void fixedPdfProducesTheExpectedPageHeadingAndBlockMetadata() throws IOException {
        Path fixture = Path.of("evaluation", "corpus", "java", "structured-java-notes.pdf");
        PdfKnowledgeDocumentExtractor extractor = new PdfKnowledgeDocumentExtractor();
        PdfKnowledgeDocumentExtraction extraction = extractor.extract(new MockMultipartFile(
                "file", fixture.getFileName().toString(), "application/pdf", Files.readAllBytes(fixture)));

        List<MarkdownChunk> chunks = new MarkdownSectionChunker().chunk(
                extractor.toSections(FixedRagStructureEvaluationDataset.STRUCTURED_PDF, extraction.blocks()),
                1000);

        assertTrue(chunks.stream().anyMatch(chunk -> chunk.pageNumber() == 1
                && chunk.blockType() == KnowledgeBlockType.CODE
                && chunk.headingPath().equals(List.of("1. HashMap"))
                && chunk.content().contains("TREEIFY_THRESHOLD")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.pageNumber() == 1
                && chunk.blockType() == KnowledgeBlockType.TABLE
                && chunk.headingPath().equals(List.of("1. HashMap"))
                && chunk.content().contains("initialCapacity | 16")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.pageNumber() == 2
                && chunk.blockType() == KnowledgeBlockType.TEXT
                && chunk.headingPath().equals(List.of("1. HashMap", "1.1 Treeification"))
                && chunk.content().contains("red black tree")));
        assertEquals(2, extraction.pageCount());
        assertEquals(List.of(), extraction.pagesNeedingOcr());
    }
}
