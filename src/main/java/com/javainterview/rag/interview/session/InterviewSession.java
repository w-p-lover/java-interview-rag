package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InterviewSession(
        String id,
        String targetRole,
        String resumeVersionId,
        InterviewSessionMode mode,
        InterviewPreparationProfile profile,
        InterviewConfigurationSnapshot configuration,
        List<InterviewQuestion> questions,
        int maxQuestionCount,
        List<InterviewSessionAnswer> answers,
        int currentQuestionIndex,
        InterviewSessionStatus status,
        long version,
        InterviewSessionReport report,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    private static final int MIN_QUESTION_COUNT = 3;
    private static final int MAX_QUESTION_COUNT = 12;

    public InterviewSession {
        id = requireText(id, "面试会话 ID 不能为空");
        targetRole = requireText(targetRole, "目标岗位不能为空");
        if (resumeVersionId != null) {
            resumeVersionId = requireText(resumeVersionId, "简历版本 ID 不能为空");
        }
        mode = Objects.requireNonNull(mode, "面试会话模式不能为空");
        profile = Objects.requireNonNull(profile, "面试准备画像不能为空");
        configuration = Objects.requireNonNull(configuration, "面试配置快照不能为空");
        if (configuration.mode() != mode) {
            throw new IllegalArgumentException("面试配置模式与会话模式不一致");
        }
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题不能为空"));
        if (questions.size() < MIN_QUESTION_COUNT || questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("面试会话题数必须在 3 到 12 之间");
        }
        if (maxQuestionCount < questions.size() || maxQuestionCount > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("面试题数上限必须在当前题数到 12 之间");
        }
        if (configuration.initialQuestionCount() > questions.size()
                || configuration.maxQuestionCount() != maxQuestionCount) {
            throw new IllegalArgumentException("面试配置题数与会话题数不一致");
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
        if (status == InterviewSessionStatus.COMPLETED && report == null) {
            throw new IllegalArgumentException("已完成面试会话必须包含报告");
        }
        if (status == InterviewSessionStatus.IN_PROGRESS && report != null) {
            throw new IllegalArgumentException("进行中面试会话不能包含报告");
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

    public InterviewSession(String id,
                            String targetRole,
                            InterviewSessionMode mode,
                            InterviewPreparationProfile profile,
                            List<InterviewQuestion> questions,
                            int maxQuestionCount,
                            List<InterviewSessionAnswer> answers,
                            int currentQuestionIndex,
                            InterviewSessionStatus status,
                            long version,
                            InterviewSessionReport report,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this(id, targetRole, null, mode, profile,
                InterviewConfigurationSnapshot.legacy(mode, questions.size(), maxQuestionCount),
                questions, maxQuestionCount, answers, currentQuestionIndex, status, version,
                report, createdAt, updatedAt);
    }

    public InterviewSession(String id,
                            String targetRole,
                            String resumeVersionId,
                            InterviewSessionMode mode,
                            InterviewPreparationProfile profile,
                            List<InterviewQuestion> questions,
                            int maxQuestionCount,
                            List<InterviewSessionAnswer> answers,
                            int currentQuestionIndex,
                            InterviewSessionStatus status,
                            long version,
                            InterviewSessionReport report,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this(id, targetRole, resumeVersionId, mode, profile,
                InterviewConfigurationSnapshot.legacy(mode, questions.size(), maxQuestionCount),
                questions, maxQuestionCount, answers, currentQuestionIndex, status, version,
                report, createdAt, updatedAt);
    }

    public InterviewSession(String id,
                            String targetRole,
                            InterviewSessionMode mode,
                            InterviewPreparationProfile profile,
                            InterviewConfigurationSnapshot configuration,
                            List<InterviewQuestion> questions,
                            int maxQuestionCount,
                            List<InterviewSessionAnswer> answers,
                            int currentQuestionIndex,
                            InterviewSessionStatus status,
                            long version,
                            InterviewSessionReport report,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this(id, targetRole, null, mode, profile, configuration, questions, maxQuestionCount,
                answers, currentQuestionIndex, status, version, report, createdAt, updatedAt);
    }

    public InterviewSession answer(String answer,
                                   InterviewAnswerEvaluation evaluation,
                                   InterviewQuestion followUpQuestion,
                                   LocalDateTime answeredAt) {
        if (status == InterviewSessionStatus.COMPLETED) {
            throw new InterviewSessionConflictException("面试会话已经完成");
        }
        String normalizedAnswer = requireText(answer, "回答不能为空");
        InterviewAnswerEvaluation normalizedEvaluation = Objects.requireNonNull(
                evaluation, "回答评价不能为空");
        LocalDateTime normalizedAnsweredAt = Objects.requireNonNull(answeredAt, "回答时间不能为空");
        List<InterviewQuestion> updatedQuestions = new ArrayList<>(questions);
        if (followUpQuestion != null
                && updatedQuestions.size() < maxQuestionCount
                && updatedQuestions.stream().noneMatch(existing ->
                existing.question().equals(followUpQuestion.question()))) {
            updatedQuestions.add(followUpQuestion);
        }
        List<InterviewSessionAnswer> updatedAnswers = new ArrayList<>(answers);
        updatedAnswers.add(new InterviewSessionAnswer(
                currentQuestionIndex, normalizedAnswer, normalizedEvaluation, normalizedAnsweredAt));
        int nextQuestionIndex = currentQuestionIndex + 1;
        InterviewSessionStatus nextStatus = nextQuestionIndex == updatedQuestions.size()
                ? InterviewSessionStatus.COMPLETED : InterviewSessionStatus.IN_PROGRESS;
        InterviewSessionReport nextReport = nextStatus == InterviewSessionStatus.COMPLETED
                ? InterviewSessionReport.from(updatedAnswers, normalizedAnsweredAt) : null;
        return new InterviewSession(
                id,
                targetRole,
                resumeVersionId,
                mode,
                profile,
                configuration,
                updatedQuestions,
                maxQuestionCount,
                updatedAnswers,
                nextQuestionIndex,
                nextStatus,
                version + 1,
                nextReport,
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
