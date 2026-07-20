package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkdownSectionChunkerTest {

    private final MarkdownSectionChunker chunker = new MarkdownSectionChunker();

    @Test
    void packsParagraphsWithinLimitAndKeepsMetadataAndGlobalOrder() {
        List<MarkdownSection> sections = List.of(
                new MarkdownSection("java/basic.md", 0, 1, List.of("Java"), "第一段\n\n第二段"),
                new MarkdownSection("java/basic.md", 1, 2, List.of("Java", "集合"), "第三段")
        );

        List<MarkdownChunk> chunks = chunker.chunk(sections, 8);

        assertEquals(2, chunks.size());
        assertChunk(chunks.get(0), "java/basic.md", 0, 0, 1, List.of("Java"), "第一段\n\n第二段");
        assertChunk(chunks.get(1), "java/basic.md", 1, 1, 2, List.of("Java", "集合"), "第三段");
    }

    @Test
    void startsNewChunkWhenNextParagraphWouldExceedLimit() {
        MarkdownSection section = new MarkdownSection(
                "java/basic.md", 0, 1, List.of("Java"), "12345\n\n6789");

        List<MarkdownChunk> chunks = chunker.chunk(List.of(section), 8);

        assertEquals(2, chunks.size());
        assertEquals("12345", chunks.get(0).content());
        assertEquals("6789", chunks.get(1).content());
    }

    @Test
    void splitsOversizedParagraphAndSkipsEmptySections() {
        List<MarkdownSection> sections = List.of(
                new MarkdownSection("java/basic.md", 0, 1, List.of("Java"), ""),
                new MarkdownSection("java/basic.md", 1, 2, List.of("Java", "集合"), "123456789")
        );

        List<MarkdownChunk> chunks = chunker.chunk(sections, 4);

        assertEquals(3, chunks.size());
        assertChunk(chunks.get(0), "java/basic.md", 0, 1, 2, List.of("Java", "集合"), "1234");
        assertChunk(chunks.get(1), "java/basic.md", 1, 1, 2, List.of("Java", "集合"), "5678");
        assertChunk(chunks.get(2), "java/basic.md", 2, 1, 2, List.of("Java", "集合"), "9");
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> chunker.chunk(null, 100));
        assertThrows(IllegalArgumentException.class, () -> chunker.chunk(List.of(), 0));
    }

    private void assertChunk(MarkdownChunk chunk, String source, int order, int sectionOrder,
                             int headingLevel, List<String> headingPath, String content) {
        assertEquals(source, chunk.source());
        assertEquals(order, chunk.order());
        assertEquals(sectionOrder, chunk.sectionOrder());
        assertEquals(headingLevel, chunk.headingLevel());
        assertEquals(headingPath, chunk.headingPath());
        assertEquals(content, chunk.content());
    }
}
