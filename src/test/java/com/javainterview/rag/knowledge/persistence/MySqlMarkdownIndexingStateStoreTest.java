package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlMarkdownIndexingStateStoreTest {

    private final KnowledgeIndexingStateMapper mapper = mock(KnowledgeIndexingStateMapper.class);
    private final MySqlMarkdownIndexingStateStore store = new MySqlMarkdownIndexingStateStore(mapper);
    private final MarkdownDocument document = MarkdownDocument.create("java/index.md", "索引内容");

    @Test
    void persistsIndexingReadyAndFailedStates() {
        store.markIndexing(document, 3);
        store.markReady(document, 3);
        store.markFailed(document, 3, new IllegalStateException("索引失败"));

        verify(mapper).upsert(document.source(), document.version(), 3, "INDEXING", null);
        verify(mapper).upsert(document.source(), document.version(), 3, "READY", null);
        verify(mapper).upsert(document.source(), document.version(), 3,
                "FAILED", "IllegalStateException");
    }

    @Test
    void restoresPersistedStateAfterProcessRestart() {
        when(mapper.find(document.source(), document.version())).thenReturn(
                new KnowledgeIndexingStateEntity(document.source(), document.version(), 3,
                        "FAILED", "IllegalStateException"));

        var state = store.find(document.source(), document.version()).orElseThrow();

        assertEquals(MarkdownIndexingStatus.FAILED, state.status());
        assertEquals(3, state.chunkCount());
        assertEquals("IllegalStateException", state.errorType());
    }

    @Test
    void returnsEmptyForUnknownStateAndDeletesAllStatesForSource() {
        assertTrue(store.find(document.source(), document.version()).isEmpty());

        store.removeBySource(document.source());

        verify(mapper).deleteBySource(document.source());
    }
}
