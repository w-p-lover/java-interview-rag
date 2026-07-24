package com.javainterview.rag.knowledge.rag;

import java.util.List;

public interface RagRetrievalLogStore {

    void record(RagRetrievalLog log);

    List<RagRetrievalLog> findRecent(int limit);
}
