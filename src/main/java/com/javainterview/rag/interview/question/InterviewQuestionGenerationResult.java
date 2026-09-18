package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionGenerationResult(
        String targetRole,
        String promptVersion,
        List<InterviewQuestion> questions,
        InterviewConfigurationSnapshot configuration,
        List<InterviewQuestionSource> unavailableSources
) {

    private static final int MAX_QUESTION_COUNT = 8;

    public InterviewQuestionGenerationResult {
        targetRole = requireText(targetRole, "目标岗位不能为空");
        promptVersion = requireText(promptVersion, "Prompt 版本不能为空");
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题不能为空"));
        if (questions.isEmpty() || questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("首轮面试题数量必须在 1 到 8 之间");
        }
        configuration = Objects.requireNonNull(configuration, "面试配置快照不能为空");
        unavailableSources = List.copyOf(Objects.requireNonNull(
                unavailableSources, "不可用题目来源不能为空"));
    }

    public InterviewQuestionGenerationResult(String targetRole,
                                             String promptVersion,
                                             List<InterviewQuestion> questions,
                                             InterviewConfigurationSnapshot configuration) {
        this(targetRole, promptVersion, questions, configuration, List.of());
    }

    public InterviewQuestionGenerationResult(String targetRole,
                                             String promptVersion,
                                             List<InterviewQuestion> questions) {
        this(targetRole, promptVersion, questions,
                InterviewConfigurationSnapshot.legacy(
                        com.javainterview.rag.interview.session.InterviewSessionMode.STANDARD,
                        questions == null ? 0 : questions.size(),
                        questions == null ? 0 : Math.max(questions.size(), 8)),
                List.of());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
