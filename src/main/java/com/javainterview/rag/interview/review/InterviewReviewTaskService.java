package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.question.InterviewQuestion;
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

    public InterviewReviewTaskService(InterviewSessionStore sessionStore,
                                      InterviewReviewTaskStore taskStore) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "面试会话存储不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "复习任务存储不能为空");
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
