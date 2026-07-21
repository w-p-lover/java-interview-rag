package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFileImporterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void importsUtf8FileAndReturnsTraceableChunks() throws IOException {
        Path file = writeFile("""
                # Java
                第一段

                第二段
                ## 集合
                HashMap
                """);
        MarkdownFileImporter importer = new MarkdownFileImporter(new InMemoryMarkdownImportRegistry(), 8);

        MarkdownImportResult result = importer.importFile(file, "java/basic.md");

        assertTrue(result.imported());
        assertEquals("java/basic.md", result.document().source());
        assertEquals(2, result.chunks().size());
        assertChunk(result.chunks().get(0), 0, 0, List.of("Java"), "第一段\n\n第二段");
        assertChunk(result.chunks().get(1), 1, 1, List.of("Java", "集合"), "HashMap");
    }

    @Test
    void skipsPreviouslyImportedSourceAndVersion() throws IOException {
        Path file = writeFile("# Java\n集合");
        MarkdownFileImporter importer = new MarkdownFileImporter(new InMemoryMarkdownImportRegistry(), 100);

        MarkdownImportResult first = importer.importFile(file, "java/basic.md");
        MarkdownImportResult duplicate = importer.importFile(file, "java/basic.md");

        assertTrue(first.imported());
        assertFalse(duplicate.imported());
        assertEquals(first.document().version(), duplicate.document().version());
        assertTrue(duplicate.chunks().isEmpty());
    }

    @Test
    void importsChangedContentForSameSource() throws IOException {
        Path file = writeFile("# Java\n原内容");
        MarkdownFileImporter importer = new MarkdownFileImporter(new InMemoryMarkdownImportRegistry(), 100);
        MarkdownImportResult original = importer.importFile(file, "java/basic.md");
        Files.writeString(file, "# Java\n新内容", StandardCharsets.UTF_8);

        MarkdownImportResult changed = importer.importFile(file, "java/basic.md");

        assertTrue(changed.imported());
        assertNotEquals(original.document().version(), changed.document().version());
        assertEquals("新内容", changed.chunks().get(0).content());
    }

    @Test
    void rejectsInvalidMaxChunkLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarkdownFileImporter(new InMemoryMarkdownImportRegistry(), 0));
    }

    private Path writeFile(String content) throws IOException {
        return Files.writeString(tempDirectory.resolve("basic.md"), content, StandardCharsets.UTF_8);
    }

    private void assertChunk(MarkdownChunk chunk, int order, int sectionOrder,
                             List<String> headingPath, String content) {
        assertEquals(order, chunk.order());
        assertEquals(sectionOrder, chunk.sectionOrder());
        assertEquals(headingPath, chunk.headingPath());
        assertEquals(content, chunk.content());
    }
}
