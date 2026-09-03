package com.javainterview.rag.knowledge.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagAnswerCitationValidatorTest {

    private final RagAnswerCitationValidator validator = new RagAnswerCitationValidator();

    @Test
    void acceptsClaimsCitingExistingSources() {
        RagAnswerCitationValidation result = validator.validate(
                "HashMap 平均查询复杂度为 O(1) [资料 1]。冲突严重时可能树化 [资料 2]。", 2);

        assertTrue(result.valid());
        assertEquals(List.of(1, 2), result.citedSourceNumbers());
        assertEquals(List.of(), result.invalidSourceNumbers());
        assertEquals(List.of(RagAnswerClaimCitationStatus.VALID_CITATION,
                        RagAnswerClaimCitationStatus.VALID_CITATION),
                result.claims().stream().map(RagAnswerClaimCitation::status).toList());
    }

    @Test
    void rejectsNonexistentCitationNumber() {
        RagAnswerCitationValidation result = validator.validate("HashMap 会树化 [资料 3]。", 2);

        assertFalse(result.valid());
        assertEquals(List.of(3), result.invalidSourceNumbers());
        assertEquals(RagAnswerClaimCitationStatus.INVALID_CITATION, result.claims().get(0).status());
    }

    @Test
    void rejectsAnUncitedClaimEvenWhenAnotherClaimHasValidCitation() {
        RagAnswerCitationValidation result = validator.validate(
                "HashMap 平均查询复杂度为 O(1) [资料 1]。并发写入也绝对安全。", 1);

        assertFalse(result.valid());
        assertEquals(List.of(RagAnswerClaimCitationStatus.VALID_CITATION,
                        RagAnswerClaimCitationStatus.MISSING_CITATION),
                result.claims().stream().map(RagAnswerClaimCitation::status).toList());
    }

    @Test
    void ignoresFormattingThatContainsNoClaimText() {
        RagAnswerCitationValidation result = validator.validate(
                "- HashMap 使用数组和链表 [资料 1]。\n\n[资料 1]", 1);

        assertTrue(result.valid());
        assertEquals(1, result.claims().size());
    }

    @Test
    void ignoresMarkdownHeadingsAndAnswerLeadInsWithoutIgnoringFacts() {
        RagAnswerCitationValidation result = validator.validate(
                "根据给定资料，回答如下：\n\n**HashMap 特点：**\nHashMap 使用数组和链表 [资料 1]。", 1);

        assertTrue(result.valid());
        assertEquals(1, result.claims().size());
        assertEquals("HashMap 使用数组和链表 [资料 1]。", result.claims().get(0).claim());
    }
}
