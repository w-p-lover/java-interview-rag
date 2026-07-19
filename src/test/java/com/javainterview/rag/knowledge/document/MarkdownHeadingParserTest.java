package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownHeadingParserTest {

    private final MarkdownHeadingParser parser = new MarkdownHeadingParser();

    @Test
    void parsesHeadingHierarchyAndKeepsSourceOrderAndParagraphs() {
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", """
                导语

                # Java
                基础说明

                ## 集合
                ArrayList

                HashMap
                ### 并发修改
                fail-fast
                ## JVM
                内存模型
                """);

        List<MarkdownSection> sections = parser.parse(document);

        assertEquals(5, sections.size());
        assertSection(sections.get(0), "java/basic.md", 0, 0, List.of(), "导语");
        assertSection(sections.get(1), "java/basic.md", 1, 1, List.of("Java"), "基础说明");
        assertSection(sections.get(2), "java/basic.md", 2, 2, List.of("Java", "集合"),
                "ArrayList\n\nHashMap");
        assertSection(sections.get(3), "java/basic.md", 3, 3, List.of("Java", "集合", "并发修改"),
                "fail-fast");
        assertSection(sections.get(4), "java/basic.md", 4, 2, List.of("Java", "JVM"), "内存模型");
    }

    @Test
    void keepsHeadingSectionsEvenWhenBodyIsEmpty() {
        MarkdownDocument document = MarkdownDocument.create("java/empty.md", """
                # Java
                ## 集合
                # JVM
                """);

        List<MarkdownSection> sections = parser.parse(document);

        assertEquals(3, sections.size());
        assertSection(sections.get(0), "java/empty.md", 0, 1, List.of("Java"), "");
        assertSection(sections.get(1), "java/empty.md", 1, 2, List.of("Java", "集合"), "");
        assertSection(sections.get(2), "java/empty.md", 2, 1, List.of("JVM"), "");
    }

    @Test
    void ignoresHeadingMarkersInsideFencedCodeBlocks() {
        MarkdownDocument document = MarkdownDocument.create("java/code.md", """
                # Java
                ```java
                # 这不是标题
                ```
                正文
                ~~~~text
                ## 这也不是标题
                ~~~~
                ## 集合 ##
                内容
                """);

        List<MarkdownSection> sections = parser.parse(document);

        assertEquals(2, sections.size());
        assertSection(sections.get(0), "java/code.md", 0, 1, List.of("Java"), """
                ```java
                # 这不是标题
                ```
                正文
                ~~~~text
                ## 这也不是标题
                ~~~~""");
        assertSection(sections.get(1), "java/code.md", 1, 2, List.of("Java", "集合"), "内容");
    }

    @Test
    void returnsNoSectionsForBlankDocument() {
        MarkdownDocument document = MarkdownDocument.create("java/blank.md", " \n\n\t");

        assertTrue(parser.parse(document).isEmpty());
    }

    private void assertSection(MarkdownSection section, String source, int order, int headingLevel,
                               List<String> headingPath, String content) {
        assertEquals(source, section.source());
        assertEquals(order, section.order());
        assertEquals(headingLevel, section.headingLevel());
        assertEquals(headingPath, section.headingPath());
        assertEquals(content, section.content());
    }
}
