package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.question.InterviewSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InterviewSkillRubric(
        String version,
        InterviewSkill skill,
        String technicalFocus,
        String technicalCriteria,
        String completenessFocus,
        String completenessCriteria,
        String communicationFocus,
        String communicationCriteria
) {

    private static final int WEAK_SCORE_THRESHOLD = 80;

    public InterviewSkillRubric {
        version = requireText(version, "专项评分规则版本不能为空");
        skill = Objects.requireNonNull(skill, "专项评分 Skill 不能为空");
        technicalFocus = requireText(technicalFocus, "技术评分关注点不能为空");
        technicalCriteria = requireText(technicalCriteria, "技术评分标准不能为空");
        completenessFocus = requireText(completenessFocus, "完整性评分关注点不能为空");
        completenessCriteria = requireText(completenessCriteria, "完整性评分标准不能为空");
        communicationFocus = requireText(communicationFocus, "表达评分关注点不能为空");
        communicationCriteria = requireText(communicationCriteria, "表达评分标准不能为空");
    }

    public List<String> weakDimensions(InterviewAnswerEvaluation evaluation) {
        Objects.requireNonNull(evaluation, "回答评价不能为空");
        List<String> dimensions = new ArrayList<>(3);
        if (evaluation.technicalScore() < WEAK_SCORE_THRESHOLD) {
            dimensions.add(technicalFocus);
        }
        if (evaluation.completenessScore() < WEAK_SCORE_THRESHOLD) {
            dimensions.add(completenessFocus);
        }
        if (evaluation.communicationScore() < WEAK_SCORE_THRESHOLD) {
            dimensions.add(communicationFocus);
        }
        if (dimensions.isEmpty()) {
            dimensions.add(lowestScoreFocus(evaluation));
        }
        return List.copyOf(dimensions.stream().distinct().toList());
    }

    public String promptSection() {
        return "\n本场专项评分规则（" + version + "，Skill=" + skill.name() + "）："
                + "\n- technicalScore / " + technicalFocus + "：" + technicalCriteria
                + "\n- completenessScore / " + completenessFocus + "：" + completenessCriteria
                + "\n- communicationScore / " + communicationFocus + "：" + communicationCriteria
                + "\nstrengths、gaps 和改进建议必须优先使用以上专项维度说明依据。";
    }

    private String lowestScoreFocus(InterviewAnswerEvaluation evaluation) {
        int lowestScore = Math.min(evaluation.technicalScore(),
                Math.min(evaluation.completenessScore(), evaluation.communicationScore()));
        if (lowestScore == evaluation.technicalScore()) {
            return technicalFocus;
        }
        if (lowestScore == evaluation.completenessScore()) {
            return completenessFocus;
        }
        return communicationFocus;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
