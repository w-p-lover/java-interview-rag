package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeDocumentOutlineBuilderTest {

    private final MarkdownHeadingParser parser = new MarkdownHeadingParser();
    private final MarkdownSectionChunker chunker = new MarkdownSectionChunker();
    private final KnowledgeDocumentOutlineBuilder builder = new KnowledgeDocumentOutlineBuilder();

    @Test
    void buildsNestedOutlineAndIgnoresPreamble() {
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", """
                标题前导正文
                # Java
                第一段

                第二段
                ### 集合
                ## 并发
                线程池
                """);
        List<MarkdownSection> sections = parser.parse(document);
        List<MarkdownChunk> chunks = chunker.chunk(sections, 4);

        KnowledgeDocumentOutline outline = builder.build(sections, chunks);

        assertEquals(3, outline.headingCount());
        assertEquals(chunks.size(), outline.chunkCount());
        assertEquals(1, outline.roots().size());
        KnowledgeDocumentOutlineNode java = outline.roots().get(0);
        assertEquals("Java", java.title());
        assertEquals(1, java.level());
        assertEquals(2, java.firstChunkOrder());
        assertEquals(2, java.directChunkCount());
        assertEquals(List.of("集合", "并发"),
                java.children().stream().map(KnowledgeDocumentOutlineNode::title).toList());
        assertNull(java.children().get(0).firstChunkOrder());
        assertEquals(0, java.children().get(0).directChunkCount());
        assertEquals(4, java.children().get(1).firstChunkOrder());
        assertEquals(1, java.children().get(1).directChunkCount());
    }

    @Test
    void keepsSkippedHeadingLevelUnderTheNearestParent() {
        MarkdownDocument document = MarkdownDocument.create("java/jvm.md", """
                ## JVM
                内容
                #### GC
                回收
                # 集合
                容器
                """);
        List<MarkdownSection> sections = parser.parse(document);
        List<MarkdownChunk> chunks = chunker.chunk(sections, 100);

        KnowledgeDocumentOutline outline = builder.build(sections, chunks);

        assertEquals(List.of("JVM", "集合"),
                outline.roots().stream().map(KnowledgeDocumentOutlineNode::title).toList());
        assertEquals("GC", outline.roots().get(0).children().get(0).title());
        assertEquals(4, outline.roots().get(0).children().get(0).level());
    }
}
