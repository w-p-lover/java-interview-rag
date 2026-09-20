package com.javainterview.rag.interview.question;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterviewSkillTrainingCatalogTest {

    private final InterviewSkillTrainingCatalog catalog = new InterviewSkillTrainingCatalog();

    @Test
    void allocatesBaselineQuestionsAcrossTheThreeSkillDimensions() {
        InterviewSkillTrainingProfile.TrainingPlan plan = catalog.plan(
                InterviewSkill.JAVA_CORE, 3, null);

        assertEquals("interview-skill-training-v1", plan.version());
        assertEquals(List.of("原理与实现", "并发与边界", "结论与推理表达"),
                plan.categories().stream().map(
                        InterviewSkillTrainingProfile.CategoryQuota::category).toList());
        assertEquals(List.of(1, 1, 1), plan.categories().stream().map(
                InterviewSkillTrainingProfile.CategoryQuota::questionCount).toList());
    }

    @Test
    void prioritizesTheWeakDimensionWithoutDroppingDeterminism() {
        InterviewSkillTrainingProfile.TrainingPlan plan = catalog.plan(
                InterviewSkill.RAG_AI, 3, "边界、评估与恢复");

        assertEquals("边界、评估与恢复", plan.focusDimension());
        assertEquals(List.of("检索生成正确性", "边界、评估与恢复", "证据与取舍表达"),
                plan.categories().stream().map(
                        InterviewSkillTrainingProfile.CategoryQuota::category).toList());
        assertEquals(List.of(1, 2, 0), plan.categories().stream().map(
                InterviewSkillTrainingProfile.CategoryQuota::questionCount).toList());
    }

    @Test
    void rejectsUnknownFocusAndGeneralSkill() {
        assertThrows(IllegalArgumentException.class,
                () -> catalog.plan(InterviewSkill.JAVA_CORE, 3, "不存在的维度"));
        assertThrows(IllegalArgumentException.class,
                () -> catalog.plan(InterviewSkill.GENERAL, 3, null));
    }
}
