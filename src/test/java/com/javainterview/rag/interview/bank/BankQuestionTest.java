package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankQuestionTest {
    @Test
    void contentKeyNormalizesPunctuationAndWhitespace() {
        BankQuestion left = question("Java 内存模型？");
        BankQuestion right = question(" Java  内存模型 ");

        assertEquals(BankQuestionKeys.contentKey(left), BankQuestionKeys.contentKey(right));
    }

    @Test
    void evidenceMustBelongToTheBoundDocumentVersion() {
        MarkdownSearchResult wrongVersion = new MarkdownSearchResult(
                "正文", "java.md", "v2", 0, List.of("Java"), null, null, KnowledgeBlockType.TEXT);

        assertThrows(IllegalArgumentException.class, () -> new BankQuestion(
                "q-1", null, "java.md", "v1", InterviewSkill.JAVA_CORE,
                InterviewDifficulty.BASIC, "内存", "题目", "目标", "答案", List.of("要点"),
                "评分", List.of(), List.of(wrongVersion), BankQuestion.Status.DRAFT,
                0, 0, null, LocalDateTime.now(), LocalDateTime.now()));
    }

    private BankQuestion question(String value) {
        MarkdownSearchResult evidence = new MarkdownSearchResult(
                "正文", "java.md", "v1", 0, List.of("Java"), null);
        return new BankQuestion("q-" + value.hashCode(), null, "java.md", "v1",
                InterviewSkill.JAVA_CORE, InterviewDifficulty.BASIC, "内存", value, "目标",
                "答案", List.of("要点"), "评分", List.of(), List.of(evidence),
                BankQuestion.Status.DRAFT, 0, 0, null, LocalDateTime.now(), LocalDateTime.now());
    }
}
