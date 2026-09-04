package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void prefersSentenceBoundaryForLongText() {
        MarkdownSection section = new MarkdownSection(
                "java/basic.md", 0, 1, List.of("Java"), "第一句说明。第二句说明！第三句");

        List<MarkdownChunk> chunks = chunker.chunk(List.of(section), 8);

        assertEquals(List.of("第一句说明。", "第二句说明！", "第三句"),
                chunks.stream().map(MarkdownChunk::content).toList());
    }

    @Test
    void keepsCodeAndTableRowsTogetherWhenSplittingStructuredBlocks() {
        MarkdownSection code = new MarkdownSection("java/code.pdf", 0, 1, List.of("Java"),
                "int a = 1;\nint b = 2;\nint c = 3;", 1, KnowledgeBlockType.CODE);
        MarkdownSection table = new MarkdownSection("java/table.pdf", 0, 1, List.of("Java"),
                "名称 | 值\nHashMap | 16\nTreeMap | 32", 1, KnowledgeBlockType.TABLE);

        List<MarkdownChunk> codeChunks = chunker.chunk(List.of(code), 18);
        List<MarkdownChunk> tableChunks = chunker.chunk(List.of(table), 14);

        assertEquals(List.of("int a = 1;\n", "int b = 2;\n", "int c = 3;"),
                codeChunks.stream().map(MarkdownChunk::content).toList());
        assertEquals(List.of("名称 | 值\n", "HashMap | 16\n", "TreeMap | 32"),
                tableChunks.stream().map(MarkdownChunk::content).toList());
    }

    @Test
    void classifiesMarkdownCodeFencesAndTablesWithoutChangingHeadingMetadata() {
        MarkdownSection section = new MarkdownSection("java/mixed.md", 0, 2,
                List.of("Java", "集合"), """
                普通说明，表达式 a | b 仍属于正文。

                ```java
                Map<String, Integer> counts = new HashMap<>();
                ```

                | 实现 | 场景 |
                | --- | --- |
                | HashMap | 单线程 |

                表格后的结论。
                """);

        List<MarkdownChunk> chunks = chunker.chunk(List.of(section), 200);

        assertEquals(List.of(KnowledgeBlockType.TEXT, KnowledgeBlockType.CODE,
                        KnowledgeBlockType.TABLE, KnowledgeBlockType.TEXT),
                chunks.stream().map(MarkdownChunk::blockType).toList());
        assertEquals(List.of("普通说明，表达式 a | b 仍属于正文。",
                        "```java\nMap<String, Integer> counts = new HashMap<>();\n```",
                        "| 实现 | 场景 |\n| --- | --- |\n| HashMap | 单线程 |",
                        "表格后的结论。"),
                chunks.stream().map(MarkdownChunk::content).toList());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.sectionOrder() == 0
                && chunk.headingPath().equals(List.of("Java", "集合"))));
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
