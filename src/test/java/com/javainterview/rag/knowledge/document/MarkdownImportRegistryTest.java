package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void findsOnlyTheRegisteredSourceAndVersion() {
        MarkdownDocument registered = MarkdownDocument.create("java/basic.md", "已登记");
        registry.register(registered);

        assertTrue(registry.isRegistered(registered));
        assertFalse(registry.isRegistered(MarkdownDocument.create("JAVA/basic.md", "已登记")));
        assertFalse(registry.isRegistered(MarkdownDocument.create("java/basic.md", "新内容")));
    }

    @Test
    void removesAllVersionsForTheSameSource() {
        MarkdownDocument original = MarkdownDocument.create("java/basic.md", "原内容");
        MarkdownDocument changed = MarkdownDocument.create("java/basic.md", "新内容");
        MarkdownDocument otherSource = MarkdownDocument.create("JAVA/BASIC.md", "其他内容");
        registry.register(original);
        registry.register(changed);
        registry.register(otherSource);

        assertTrue(registry.unregisterBySource("java/basic.md"));
        assertTrue(registry.register(original));
        assertTrue(registry.register(changed));
        assertFalse(registry.register(otherSource));
    }

    @Test
    void listsCurrentVersionAndRestoresPreviousVersionAfterRollback() {
        MarkdownDocument original = MarkdownDocument.create("java/basic.md", "原内容");
        MarkdownDocument changed = MarkdownDocument.create("java/basic.md", "新内容");
        MarkdownDocument other = MarkdownDocument.create("java/jvm.md", "JVM 内容");
        registry.register(original);
        registry.register(changed);
        registry.register(other);

        assertEquals(2, registry.findCurrentDocuments().size());
        assertEquals(changed.version(), findCurrent("java/basic.md").version());
        assertEquals(changed.content().length(), findCurrent("java/basic.md").contentLength());

        assertTrue(registry.unregister(changed));
        assertEquals(original.version(), findCurrent("java/basic.md").version());
    }

    @Test
    void listsAllVersionsForTheExactSourceFromNewestToOldest() {
        MarkdownDocument original = MarkdownDocument.create("java/basic.md", "原内容");
        MarkdownDocument changed = MarkdownDocument.create("java/basic.md", "新内容");
        MarkdownDocument other = MarkdownDocument.create("JAVA/basic.md", "其他内容");
        registry.register(original);
        registry.register(changed);
        registry.register(other);

        var versions = registry.findVersionsBySource("java/basic.md");

        assertEquals(List.of(changed.version(), original.version()),
                versions.stream().map(KnowledgeDocumentSummary::version).toList());
    }

    private KnowledgeDocumentSummary findCurrent(String source) {
        return registry.findCurrentDocuments().stream()
                .filter(document -> document.source().equals(source))
                .findFirst()
                .orElseThrow();
    }
}
