package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.question.InterviewSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewSkillRubricCatalogTest {

    private final InterviewSkillRubricCatalog catalog = new InterviewSkillRubricCatalog();

    @Test
    void providesVersionedSkillSpecificEvaluationFocuses() {
        InterviewSkillRubric javaRubric = catalog.forSkill(InterviewSkill.JAVA_CORE);
        InterviewSkillRubric systemDesignRubric = catalog.forSkill(InterviewSkill.SYSTEM_DESIGN);

        assertEquals("interview-skill-rubric-v1", javaRubric.version());
        assertEquals(InterviewSkill.JAVA_CORE, javaRubric.skill());
        assertNotEquals(javaRubric.completenessFocus(), systemDesignRubric.completenessFocus());
    }

    @Test
    void mapsSubThresholdScoresToSkillSpecificWeakDimensions() {
        InterviewAnswerEvaluation evaluation = new InterviewAnswerEvaluation(
                84, 62, 79, 75,
                List.of("说明了主体流程"), List.of("缺少并发边界"),
                "补充并发竞争和异常恢复。", false);

        assertEquals(List.of("并发与边界", "结论与推理表达"),
                catalog.forSkill(InterviewSkill.JAVA_CORE).weakDimensions(evaluation));
    }

    @Test
    void resolvesPersistedVersionAndRejectsUnknownReviewFocus() {
        InterviewSkillRubric rubric = catalog.forVersion(
                InterviewSkill.RAG_AI, "interview-skill-rubric-v1");

        assertTrue(rubric.promptSection(List.of("边界、评估与恢复"))
                .contains("本次复习重点：边界、评估与恢复"));
        assertThrows(IllegalArgumentException.class,
                () -> rubric.promptSection(List.of("不存在的维度")));
        assertThrows(IllegalArgumentException.class,
                () -> catalog.forVersion(InterviewSkill.RAG_AI, "interview-skill-rubric-v0"));
    }
}
