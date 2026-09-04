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

    @Test
    void supportsAdjacentEvidenceWithoutTreatingContrastiveNegationAsConflict() {
        RagAnswerEvidenceValidation result = validator.validate(
                "当哈希桶内节点数量达到树化阈值 8 时，如果当前数组容量还小于 64，"
                        + "HashMap 会优先扩容而不是立即转换成红黑树 [资料 1]。"
                        + "优先扩容的目的是通过重新分布节点来减少冲突 [资料 1]。",
                List.of(source("Java 8 中，冲突桶开始使用链表保存节点。"
                        + "当桶内节点数量达到树化阈值 8，并且当前数组容量至少为 64 时，"
                        + "该桶可以转换为红黑树。若数组容量还小于 64，HashMap 会优先扩容，"
                        + "而不是立即树化，以便通过重新分布减少冲突。")));

        assertTrue(result.valid());
        assertEquals(2, result.claims().size());
        assertTrue(result.claims().stream()
                .allMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.SUPPORTED));
    }

    private MarkdownSearchResult source(String content) {
        return new MarkdownSearchResult(content, "java/collections.md", "version-1", 0,
                List.of("Java", "集合"), 0.91);
    }
}
