package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.evaluation.InterviewAnswerPreviousAttempt;
import com.javainterview.rag.interview.evaluation.InterviewAnswerAssessment;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionAnswer;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class InterviewReviewTaskService {

    private final InterviewSessionStore sessionStore;
    private final InterviewReviewTaskStore taskStore;
    private final InterviewReviewAttemptStore attemptStore;
    private final InterviewAnswerEvaluator answerEvaluator;

    public InterviewReviewTaskService(InterviewSessionStore sessionStore,
                                      InterviewReviewTaskStore taskStore) {
        this(sessionStore, taskStore, null, null);
    }

    public InterviewReviewTaskService(InterviewSessionStore sessionStore,
                                      InterviewReviewTaskStore taskStore,
                                      InterviewReviewAttemptStore attemptStore,
                                      InterviewAnswerEvaluator answerEvaluator) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "面试会话存储不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "复习任务存储不能为空");
        this.attemptStore = attemptStore;
        this.answerEvaluator = answerEvaluator;
    }

    public List<InterviewReviewTask> generate(String sessionId) {
        InterviewSession session = findSession(sessionId);
        if (session.status() != InterviewSessionStatus.COMPLETED) {
            throw new IllegalArgumentException("面试会话尚未完成，不能生成复习任务");
        }
        LocalDateTime generatedAt = LocalDateTime.now();
        for (InterviewSessionAnswer answer : session.answers()) {
            if (answer.evaluation().gaps().isEmpty()) {
                continue;
            }
            InterviewQuestion question = session.questions().get(answer.questionIndex());
            taskStore.createIfAbsent(toTask(session, question, answer, generatedAt));
        }
        return taskStore.findBySessionId(session.id());
    }

    public List<InterviewReviewTask> findBySessionId(String sessionId) {
        InterviewSession session = findSession(sessionId);
        return taskStore.findBySessionId(session.id());
    }

    public InterviewReviewTask complete(String sessionId, String taskId) {
        InterviewSession session = findSession(sessionId);
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("复习任务 ID 不能为空");
        }
        return taskStore.complete(session.id(), taskId.strip(), LocalDateTime.now())
                .orElseThrow(() -> new NoSuchElementException("复习任务不存在"));
    }

    public List<InterviewReviewAttempt> findAttempts(String sessionId, String taskId) {
        requireAttemptDependencies();
        InterviewSession session = findSession(sessionId);
        InterviewReviewTask task = findTask(session.id(), taskId);
        return attemptStore.findByReviewTaskId(session.id(), task.id());
    }

    public InterviewReviewAttempt submitAttempt(String sessionId,
                                                String taskId,
                                                String answer,
                                                int expectedAttemptNumber) {
        requireAttemptDependencies();
        InterviewSession session = findSession(sessionId);
        if (session.status() != InterviewSessionStatus.COMPLETED) {
            throw new IllegalArgumentException("面试会话尚未完成，不能提交复习回答");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("复习回答不能为空");
        }
        String normalizedAnswer = answer.strip();
        if (normalizedAnswer.length() > 10_000) {
            throw new IllegalArgumentException("复习回答不能超过 10000 个字符");
        }
        InterviewReviewTask task = findTask(session.id(), taskId);
        List<InterviewReviewAttempt> existing = attemptStore.findByReviewTaskId(session.id(), task.id());
        int actualAttemptNumber = existing.size() + 1;
        if (expectedAttemptNumber != actualAttemptNumber) {
            throw new InterviewReviewAttemptConflictException(
                    "复习回答序号已变化，请刷新历史后重试");
        }
        InterviewSessionAnswer originalAnswer = session.answers().get(task.questionIndex());
        InterviewQuestion question = session.questions().get(task.questionIndex());
        List<InterviewAnswerPreviousAttempt> previousAttempts = existing.stream()
                .map(attempt -> new InterviewAnswerPreviousAttempt(attempt.answer(), attempt.evaluation()))
                .toList();
        InterviewAnswerAssessment assessment = answerEvaluator.evaluateForReview(
                session.profile(), question, normalizedAnswer, previousAttempts);
        Integer previousScore = existing.isEmpty()
                ? null : existing.get(existing.size() - 1).evaluation().overallScore();
        int score = assessment.evaluation().overallScore();
        InterviewReviewAttempt attempt = new InterviewReviewAttempt(
                UUID.randomUUID().toString(),
                session.id(),
                task.id(),
                task.questionIndex(),
                actualAttemptNumber,
                normalizedAnswer,
                assessment.evaluation(),
                question.knowledgeGrounded(),
                question.knowledgeSources().stream().map(InterviewReviewKnowledgeSource::from).toList(),
                answerEvaluator.promptVersion(),
                answerEvaluator.rubricVersion(),
                answerEvaluator.modelName(),
                originalAnswer.evaluation().overallScore(),
                previousScore,
                score - originalAnswer.evaluation().overallScore(),
                previousScore == null ? null : score - previousScore,
                LocalDateTime.now());
        attemptStore.create(attempt);
        return attempt;
    }

    private InterviewReviewTask findTask(String sessionId, String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("复习任务 ID 不能为空");
        }
        return taskStore.findBySessionAndId(sessionId, taskId.strip())
                .orElseThrow(() -> new NoSuchElementException("复习任务不存在"));
    }

    private void requireAttemptDependencies() {
        if (attemptStore == null || answerEvaluator == null) {
            throw new IllegalStateException("复习回答能力尚未配置");
        }
    }

    private InterviewSession findSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("面试会话 ID 不能为空");
        }
        return sessionStore.findById(sessionId.strip())
                .orElseThrow(() -> new NoSuchElementException("面试会话不存在"));
    }

    private InterviewReviewTask toTask(InterviewSession session,
                                       InterviewQuestion question,
                                       InterviewSessionAnswer answer,
                                       LocalDateTime generatedAt) {
        String taskKey = session.id() + ":" + answer.questionIndex();
        String taskId = UUID.nameUUIDFromBytes(taskKey.getBytes(StandardCharsets.UTF_8)).toString();
        List<InterviewReviewKnowledgeSource> knowledgeSources = question.knowledgeSources().stream()
                .map(InterviewReviewKnowledgeSource::from)
                .toList();
        return new InterviewReviewTask(
                taskId,
                session.id(),
                answer.questionIndex(),
                question.question(),
                question.objective(),
                answer.evaluation().gaps(),
                answer.evaluation().improvementSuggestion(),
                question.knowledgeGrounded(),
                knowledgeSources,
                InterviewReviewTaskStatus.PENDING,
                generatedAt,
                null
        );
    }
}
