package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownImportRegistryTest {

    private final MarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();

    @Test
    void rejectsDuplicateSourceAndVersion() {
        MarkdownDocument windowsDocument = MarkdownDocument.create("java/basic.md", "# Java\r\n集合");
        MarkdownDocument unixDocument = MarkdownDocument.create("java/basic.md", "# Java\n集合");

        assertTrue(registry.register(windowsDocument));
        assertFalse(registry.register(unixDocument));
    }

    @Test
    void registersChangedContentForSameSource() {
        MarkdownDocument original = MarkdownDocument.create("java/basic.md", "原内容");
        MarkdownDocument changed = MarkdownDocument.create("java/basic.md", "新内容");

        assertTrue(registry.register(original));
        assertTrue(registry.register(changed));
    }

    @Test
    void registersSameVersionForDifferentSources() {
        MarkdownDocument basic = MarkdownDocument.create("java/basic.md", "相同内容");
        MarkdownDocument collection = MarkdownDocument.create("java/collection.md", "相同内容");

        assertTrue(registry.register(basic));
        assertTrue(registry.register(collection));
    }
}
