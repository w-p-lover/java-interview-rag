package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.rag.RagRetrievalLog;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStatus;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlRagRetrievalLogStoreTest {

    @Test
    void persistsTraceableSourceMetadataWithoutChunkContent() throws Exception {
        RagRetrievalLogMapper mapper = mock(RagRetrievalLogMapper.class);
        MySqlRagRetrievalLogStore store = new MySqlRagRetrievalLogStore(mapper, new ObjectMapper());
        RagRetrievalLog log = new RagRetrievalLog(null, "HashMap 底层结构", 5, 0.60,
                "rag-answer-v1", RagRetrievalLogStatus.ANSWERED, true,
                List.of(new RagRetrievalSource("java/collections.md", "version-1", 0,
                        List.of("Java", "集合", "HashMap"), 0.91)),
                25, null, LocalDateTime.of(2026, 7, 24, 10, 0));

        store.record(log);

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(RagRetrievalLogEntity.class);
        verify(mapper).insert(entityCaptor.capture());
        RagRetrievalLogEntity entity = entityCaptor.getValue();
        assertEquals("HashMap 底层结构", entity.getQuestion());
        assertEquals("ANSWERED", entity.getStatus());
        assertEquals("rag-answer-v1", entity.getPromptVersion());
        assertEquals("[{\"source\":\"java/collections.md\",\"documentVersion\":\"version-1\",\"chunkOrder\":0,\"headingPath\":[\"Java\",\"集合\",\"HashMap\"],\"similarity\":0.91}]",
                entity.getRetrievedSourcesJson());
    }

    @Test
    void readsRecentLogsInDescendingOrderFromMapper() {
        RagRetrievalLogMapper mapper = mock(RagRetrievalLogMapper.class);
        MySqlRagRetrievalLogStore store = new MySqlRagRetrievalLogStore(mapper, new ObjectMapper());
        RagRetrievalLog log = new RagRetrievalLog(null, "ConcurrentHashMap", 5, 0.60,
                "rag-answer-v1", RagRetrievalLogStatus.INSUFFICIENT, false,
                List.of(), 10, null, LocalDateTime.of(2026, 7, 24, 10, 1));
        RagRetrievalLogEntity entity = RagRetrievalLogEntity.from(log, "[]");
        when(mapper.findRecent(20)).thenReturn(List.of(entity));

        List<RagRetrievalLog> logs = store.findRecent(20);

        assertEquals(List.of(log), logs);
        verify(mapper).findRecent(20);
    }

    @Test
    void doesNotPropagateLogWriteFailures() {
        RagRetrievalLogMapper mapper = mock(RagRetrievalLogMapper.class);
        MySqlRagRetrievalLogStore store = new MySqlRagRetrievalLogStore(mapper, new ObjectMapper());
        when(mapper.insert(any(RagRetrievalLogEntity.class)))
                .thenThrow(new IllegalStateException("日志表不可用"));
        RagRetrievalLog log = new RagRetrievalLog(null, "HashMap", 5, 0.60,
                "rag-answer-v1", RagRetrievalLogStatus.ANSWERED, true,
                List.of(), 8, null, LocalDateTime.of(2026, 7, 24, 10, 3));

        assertDoesNotThrow(() -> store.record(log));
    }
}
