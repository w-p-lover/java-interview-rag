package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;

/** 为不同检索分数体系提供独立的拒答判定。 */
public enum RagRetrievalRejectionPolicy {

    COSINE_THRESHOLD("cosine_threshold_v1") {
        @Override
        public boolean rejected(List<RagRetrievalSource> sources, double threshold) {
            return sources.stream()
                    .map(RagRetrievalSource::similarity)
                    .filter(java.util.Objects::nonNull)
                    .noneMatch(similarity -> similarity >= threshold);
        }

        @Override
        public double reportThreshold() {
            return 0.60;
        }
    },

    CANDIDATE_PRESENCE("candidate_presence_v1") {
        @Override
        public boolean rejected(List<RagRetrievalSource> sources, double threshold) {
            return sources.isEmpty();
        }

        @Override
        public double reportThreshold() {
            return 0.0;
        }
    };

    private final String code;

    RagRetrievalRejectionPolicy(String code) {
        this.code = code;
    }

    public abstract boolean rejected(List<RagRetrievalSource> sources, double threshold);

    public abstract double reportThreshold();

    public String code() {
        return code;
    }
}
