package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkdownDocumentTest {

    @Test
    void createsDocumentWithStableSha256Version() {
        MarkdownDocument document = MarkdownDocument.create("java/basic.md", "abc");

        assertEquals("java/basic.md", document.source());
        assertEquals("abc", document.content());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                document.version());
    }

    @Test
    void normalizesLineEndingsWhenCalculatingVersion() {
        MarkdownDocument windowsDocument = MarkdownDocument.create("java/windows.md", "# Java\r\n集合");
        MarkdownDocument unixDocument = MarkdownDocument.create("java/unix.md", "# Java\n集合");

        assertEquals(unixDocument.version(), windowsDocument.version());
    }

    @Test
    void changesVersionWhenContentChanges() {
        MarkdownDocument original = MarkdownDocument.create("java/basic.md", "原内容");
        MarkdownDocument changed = MarkdownDocument.create("java/basic.md", "新内容");

        assertNotEquals(original.version(), changed.version());
    }

    @Test
    void rejectsInvalidDocumentInput() {
        assertThrows(IllegalArgumentException.class, () -> MarkdownDocument.create(" ", "内容"));
        assertThrows(NullPointerException.class, () -> MarkdownDocument.create("java/basic.md", null));
    }
}
