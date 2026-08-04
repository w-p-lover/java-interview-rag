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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        return findBySessionId(sessionId, null, null, null);
    }

    public List<InterviewReviewTask> findBySessionId(String sessionId,
                                                     String status,
                                                     LocalDate plannedDateFrom,
                                                     LocalDate plannedDateTo) {
        InterviewSession session = findSession(sessionId);
        if (plannedDateFrom != null && plannedDateTo != null
                && plannedDateTo.isBefore(plannedDateFrom)) {
            throw new IllegalArgumentException("计划复习结束日期不能早于开始日期");
        }
        return taskStore.findBySessionId(
                session.id(), parseStatus(status), plannedDateFrom, plannedDateTo);
    }

    public InterviewReviewTask complete(String sessionId, String taskId) {
        InterviewSession session = findSession(sessionId);
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("复习任务 ID 不能为空");
        }
        return taskStore.complete(session.id(), taskId.strip(), LocalDateTime.now())
                .orElseThrow(() -> new NoSuchElementException("复习任务不存在"));
    }

    public InterviewReviewTask schedule(String sessionId, String taskId,
                                        LocalDate plannedReviewDate) {
        InterviewSession session = findSession(sessionId);
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("复习任务 ID 不能为空");
        }
        return taskStore.schedule(session.id(), taskId.strip(), plannedReviewDate)
                .orElseThrow(() -> new NoSuchElementException("复习任务不存在"));
    }

    public List<InterviewReviewAttempt> findAttempts(String sessionId, String taskId) {
        requireAttemptStore();
        InterviewSession session = findSession(sessionId);
        InterviewReviewTask task = findTask(session.id(), taskId);
        return attemptStore.findByReviewTaskId(session.id(), task.id());
    }

    public InterviewReviewSessionHistory findAttemptHistory(String sessionId) {
        requireAttemptStore();
        InterviewSession session = findSession(sessionId);
        List<InterviewReviewTaskHistory> taskHistories = taskStore.findBySessionId(session.id()).stream()
                .map(task -> toTaskHistory(session, task))
                .toList();
        int practicedTaskCount = (int) taskHistories.stream()
                .filter(history -> history.attemptCount() > 0)
                .count();
        int totalAttemptCount = taskHistories.stream()
                .mapToInt(InterviewReviewTaskHistory::attemptCount)
                .sum();
        int improvedTaskCount = (int) taskHistories.stream()
                .filter(history -> history.latestScoreDeltaFromOriginal() != null
                        && history.latestScoreDeltaFromOriginal() > 0)
                .count();
        return new InterviewReviewSessionHistory(
                session.id(), taskHistories.size(), practicedTaskCount,
                totalAttemptCount, improvedTaskCount, taskHistories);
    }

    public InterviewReviewAttempt submitAttempt(String sessionId,
                                                String taskId,
                                                String answer,
                                                int expectedAttemptNumber) {
        requireAttemptSubmissionDependencies();
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

    private InterviewReviewTaskHistory toTaskHistory(InterviewSession session,
                                                     InterviewReviewTask task) {
        List<InterviewReviewAttempt> attempts = attemptStore
                .findByReviewTaskId(session.id(), task.id()).stream()
                .sorted(Comparator.comparingInt(InterviewReviewAttempt::attemptNumber))
                .toList();
        int originalOverallScore = session.answers().get(task.questionIndex())
                .evaluation().overallScore();
        if (attempts.isEmpty()) {
            return new InterviewReviewTaskHistory(
                    task.id(), task.questionIndex(), 0, originalOverallScore,
                    null, null, null, null, task.gaps(), null, List.of());
        }
        InterviewReviewAttempt first = attempts.get(0);
        InterviewReviewAttempt latest = attempts.get(attempts.size() - 1);
        int bestOverallScore = attempts.stream()
                .mapToInt(attempt -> attempt.evaluation().overallScore())
                .max()
                .orElseThrow();
        return new InterviewReviewTaskHistory(
                task.id(), task.questionIndex(), attempts.size(), originalOverallScore,
                first.evaluation().overallScore(), latest.evaluation().overallScore(),
                bestOverallScore, latest.evaluation().overallScore() - originalOverallScore,
                latest.evaluation().gaps(), latest.createdAt(), attempts);
    }

    private void requireAttemptStore() {
        if (attemptStore == null) {
            throw new IllegalStateException("复习回答能力尚未配置");
        }
    }

    private InterviewReviewTaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return InterviewReviewTaskStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("复习任务状态仅支持 PENDING 或 COMPLETED");
        }
    }

    private void requireAttemptSubmissionDependencies() {
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
