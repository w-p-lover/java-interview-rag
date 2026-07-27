package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InterviewSession(
        String id,
        String targetRole,
        InterviewPreparationProfile profile,
        List<InterviewQuestion> questions,
        List<InterviewSessionAnswer> answers,
        int currentQuestionIndex,
        InterviewSessionStatus status,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    private static final int QUESTION_COUNT = 3;

    public InterviewSession {
        id = requireText(id, "面试会话 ID 不能为空");
        targetRole = requireText(targetRole, "目标岗位不能为空");
        profile = Objects.requireNonNull(profile, "面试准备画像不能为空");
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题不能为空"));
        if (questions.size() != QUESTION_COUNT) {
            throw new IllegalArgumentException("面试会话必须包含 3 道题");
        }
        answers = List.copyOf(Objects.requireNonNull(answers, "面试回答不能为空"));
        if (currentQuestionIndex < 0 || currentQuestionIndex > questions.size()) {
            throw new IllegalArgumentException("当前题号超出面试题范围");
        }
        if (answers.size() != currentQuestionIndex) {
            throw new IllegalArgumentException("回答数量与当前题号不一致");
        }
        for (int index = 0; index < answers.size(); index++) {
            if (answers.get(index).questionIndex() != index) {
                throw new IllegalArgumentException("面试回答必须按题目顺序保存");
            }
        }
        status = Objects.requireNonNull(status, "面试会话状态不能为空");
        InterviewSessionStatus expectedStatus = currentQuestionIndex == questions.size()
                ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.IN_PROGRESS;
        if (status != expectedStatus) {
            throw new IllegalArgumentException("面试会话状态与当前题号不一致");
        }
        if (version < 0) {
            throw new IllegalArgumentException("面试会话版本不能为负数");
        }
        createdAt = Objects.requireNonNull(createdAt, "面试会话创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "面试会话更新时间不能为空");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("面试会话更新时间不能早于创建时间");
        }
    }

    public InterviewSession answer(String answer, LocalDateTime answeredAt) {
        if (status == InterviewSessionStatus.COMPLETED) {
            throw new InterviewSessionConflictException("面试会话已经完成");
        }
        String normalizedAnswer = requireText(answer, "回答不能为空");
        LocalDateTime normalizedAnsweredAt = Objects.requireNonNull(answeredAt, "回答时间不能为空");
        List<InterviewSessionAnswer> updatedAnswers = new ArrayList<>(answers);
        updatedAnswers.add(new InterviewSessionAnswer(currentQuestionIndex, normalizedAnswer, normalizedAnsweredAt));
        int nextQuestionIndex = currentQuestionIndex + 1;
        InterviewSessionStatus nextStatus = nextQuestionIndex == questions.size()
                ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.IN_PROGRESS;
        return new InterviewSession(
                id,
                targetRole,
                profile,
                questions,
                updatedAnswers,
                nextQuestionIndex,
                nextStatus,
                version + 1,
                createdAt,
                normalizedAnsweredAt
        );
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
