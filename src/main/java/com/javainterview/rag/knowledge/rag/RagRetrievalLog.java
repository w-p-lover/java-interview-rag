package com.javainterview.rag.knowledge.rag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record RagRetrievalLog(Long id, String question, int topK, double similarityThreshold,
                              String promptVersion, RagRetrievalLogStatus status, boolean grounded,
                              List<RagRetrievalSource> sources, long elapsedMillis,
                              String errorType, LocalDateTime createdAt) {

    public RagRetrievalLog {
        Objects.requireNonNull(question, "问题不能为空");
        Objects.requireNonNull(promptVersion, "提示词版本不能为空");
        Objects.requireNonNull(status, "回答状态不能为空");
        sources = List.copyOf(Objects.requireNonNull(sources, "命中来源不能为空"));
        Objects.requireNonNull(createdAt, "创建时间不能为空");
    }
}
