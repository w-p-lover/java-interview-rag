package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.evaluation.InterviewSkillRubric;
import com.javainterview.rag.interview.evaluation.InterviewSkillRubricCatalog;

import java.util.List;
import java.util.Objects;

public class InterviewSkillTrainingCatalog {

    public static final String VERSION = "interview-skill-training-v1";

    private final InterviewSkillRubricCatalog rubricCatalog = new InterviewSkillRubricCatalog();

    public InterviewSkillTrainingProfile.TrainingPlan plan(
            InterviewSkill skill,
            int questionCount,
            String focusDimension) {
        return profile(skill).allocate(questionCount, focusDimension);
    }

    public InterviewSkillTrainingProfile profile(InterviewSkill skill) {
        Objects.requireNonNull(skill, "请选择 Skill");
        if (skill == InterviewSkill.GENERAL) {
            throw new IllegalArgumentException("请选择具体 Skill");
        }
        InterviewSkillRubric rubric = rubricCatalog.forSkill(skill);
        return new InterviewSkillTrainingProfile(
                VERSION,
                skill,
                List.of(
                        new InterviewSkillTrainingProfile.Category(
                                rubric.technicalFocus(), rubric.technicalCriteria()),
                        new InterviewSkillTrainingProfile.Category(
                                rubric.completenessFocus(), rubric.completenessCriteria()),
                        new InterviewSkillTrainingProfile.Category(
                                rubric.communicationFocus(), rubric.communicationCriteria())));
    }
}
