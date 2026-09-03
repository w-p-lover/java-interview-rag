package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagAnswerEvidenceValidatorTest {

    private final RagAnswerEvidenceValidator validator = new RagAnswerEvidenceValidator();

    @Test
    void classifiesAClaimWithMatchingTechnicalAnchorsAsSupported() {
        RagAnswerEvidenceValidation result = validator.validate(
                "HashMap 使用数组、链表和红黑树 [资料 1]。",
                List.of(source("HashMap 在 Java 8 中使用数组、链表和红黑树。")));

        assertTrue(result.valid());
        assertEquals(RagAnswerClaimEvidenceStatus.SUPPORTED, result.claims().get(0).status());
        assertEquals(1.0, result.claims().get(0).anchorCoverage());
    }

    @Test
    void classifiesAClaimWithoutEnoughEvidenceAsInsufficient() {
        RagAnswerEvidenceValidation result = validator.validate(
                "Redis 支持持久化 [资料 1]。",
                List.of(source("HashMap 使用数组和链表。")));

        assertFalse(result.valid());
        assertEquals(RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT,
                result.claims().get(0).status());
    }

    @Test
    void classifiesOppositeNegationForSharedAnchorsAsConflicting() {
        RagAnswerEvidenceValidation result = validator.validate(
                "普通 HashMap 提供并发安全 [资料 1]。",
                List.of(source("普通 HashMap 不提供并发安全。")));

        assertFalse(result.valid());
        assertEquals(RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING,
                result.claims().get(0).status());
        assertTrue(result.hasConflict());
    }

    private MarkdownSearchResult source(String content) {
        return new MarkdownSearchResult(content, "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
    }
}
