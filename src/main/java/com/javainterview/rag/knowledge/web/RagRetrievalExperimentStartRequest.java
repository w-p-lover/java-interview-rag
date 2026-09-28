package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentSplit;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentStrategy;

public record RagRetrievalExperimentStartRequest(
        String name,
        RagRetrievalExperimentStrategy strategy,
        RagRetrievalExperimentSplit split
) {
}
