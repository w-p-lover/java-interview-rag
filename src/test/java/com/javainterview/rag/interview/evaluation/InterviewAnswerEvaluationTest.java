package com.javainterview.rag.interview.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewAnswerEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void restoresOldJsonWithoutEvidenceFields() throws Exception {
        InterviewAnswerEvaluation evaluation = objectMapper.readValue("""
                {
                  "technicalScore":80,
                  "completenessScore":70,
                  "communicationScore":75,
                  "overallScore":75,
                  "strengths":["结论明确"],
                  "gaps":["缺少失败补偿"],
                  "improvementSuggestion":"补充异常路径。",
                  "knowledgeGrounded":true
                }
                """, InterviewAnswerEvaluation.class);

        assertTrue(evaluation.answerEvidence().isEmpty());
        assertTrue(evaluation.knowledgeEvidence().isEmpty());
    }

    @Test
    void roundTripsVerifiedEvidenceMetadata() throws Exception {
        InterviewAnswerEvaluation evaluation = new InterviewAnswerEvaluation(
                80, 70, 75, 75,
                List.of("结论明确"),
                List.of("缺少失败补偿"),
                "补充异常路径。",
                true,
                List.of("先写入新版本"),
                List.of(new InterviewKnowledgeEvidence(
                        "rag/vector-lifecycle.md",
                        "version-1",
                        2,
                        List.of("RAG", "向量生命周期"),
                        0.88,
                        "新版本成功后再清理旧版本")));

        InterviewAnswerEvaluation restored = objectMapper.readValue(
                objectMapper.writeValueAsString(evaluation), InterviewAnswerEvaluation.class);

        assertEquals(evaluation, restored);
    }

    @Test
    void rejectsNullKnowledgeEvidenceDraftItem() {
        assertThrows(IllegalArgumentException.class, () -> new InterviewAnswerEvaluationDraft(
                80, 70, 75,
                List.of("结论明确"),
                List.of("缺少失败补偿"),
                "补充异常路径。",
                List.of("先写入新版本"),
                java.util.Collections.singletonList(null),
                false, "", "", ""));
    }
}
