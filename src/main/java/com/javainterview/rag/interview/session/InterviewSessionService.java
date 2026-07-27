package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.evaluation.InterviewAnswerAssessment;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class InterviewSessionService {

    private static final int TARGET_ROLE_MAX_LENGTH = 100;
    private static final int ANSWER_MAX_LENGTH = 10_000;

    private final InterviewSessionStore store;
    private final InterviewAnswerEvaluator answerEvaluator;

    public InterviewSessionService(InterviewSessionStore store, InterviewAnswerEvaluator answerEvaluator) {
        this.store = Objects.requireNonNull(store, "面试会话存储不能为空");
        this.answerEvaluator = Objects.requireNonNull(answerEvaluator, "回答评价器不能为空");
    }

    public InterviewSession start(String targetRole, InterviewPreparationProfile profile,
                                  List<InterviewQuestion> questions) {
        if (questions == null) {
            throw new IllegalArgumentException("面试题不能为空");
        }
        return start(targetRole, profile, questions, Math.max(questions.size(), 8));
    }

    public InterviewSession start(String targetRole, InterviewPreparationProfile profile,
                                  List<InterviewQuestion> questions, int maxQuestionCount) {
        String normalizedTargetRole = requireText(targetRole, TARGET_ROLE_MAX_LENGTH,
                "目标岗位不能为空", "目标岗位不能超过 100 个字符");
        if (profile == null) {
            throw new IllegalArgumentException("面试准备画像不能为空");
        }
        if (questions == null) {
            throw new IllegalArgumentException("面试题不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        InterviewSession session = new InterviewSession(
                UUID.randomUUID().toString(),
                normalizedTargetRole,
                profile,
                questions,
                maxQuestionCount,
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                null,
                now,
                now
        );
        store.create(session);
        return session;
    }

    public InterviewSession find(String sessionId) {
        String normalizedSessionId = requireText(sessionId, 36,
                "面试会话 ID 不能为空", "面试会话 ID 不能超过 36 个字符");
        return store.findById(normalizedSessionId)
                .orElseThrow(() -> new NoSuchElementException("面试会话不存在"));
    }

    public InterviewSession submitAnswer(String sessionId, int questionIndex,
                                         long expectedVersion, String answer) {
        String normalizedAnswer = requireText(answer, ANSWER_MAX_LENGTH,
                "回答不能为空", "回答不能超过 10000 个字符");
        InterviewSession existing = find(sessionId);
        if (questionIndex != existing.currentQuestionIndex() || expectedVersion != existing.version()) {
            throw new InterviewSessionConflictException("回答请求已过期，请刷新后重试");
        }
        if (existing.status() == InterviewSessionStatus.COMPLETED) {
            throw new InterviewSessionConflictException("面试会话已经完成");
        }
        InterviewQuestion currentQuestion = existing.questions().get(existing.currentQuestionIndex());
        InterviewAnswerAssessment assessment = answerEvaluator.evaluate(
                existing.profile(), currentQuestion, normalizedAnswer);
        InterviewQuestion followUpQuestion = existing.questions().size() < existing.maxQuestionCount()
                ? assessment.followUpQuestion() : null;
        InterviewSession updated = existing.answer(
                normalizedAnswer, assessment.evaluation(), followUpQuestion, LocalDateTime.now());
        if (!store.update(updated, expectedVersion)) {
            throw new InterviewSessionConflictException("面试会话已被并发更新，请刷新后重试");
        }
        return updated;
    }

    private String requireText(String value, int maxLength, String requiredMessage, String lengthMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }
}
