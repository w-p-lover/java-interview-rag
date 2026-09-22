package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.bank.BankGenerationTask;
import com.javainterview.rag.interview.bank.BankQuestion;
import com.javainterview.rag.interview.bank.BankQuestionKeys;
import com.javainterview.rag.interview.bank.QuestionBankStore;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MySqlQuestionBankStore implements QuestionBankStore {
    private final QuestionBankMapper questions;
    private final BankGenerationTaskMapper tasks;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    public MySqlQuestionBankStore(QuestionBankMapper questions, BankGenerationTaskMapper tasks,
                                  ObjectMapper json, TransactionTemplate transactions) {
        this.questions = questions;
        this.tasks = tasks;
        this.json = json;
        this.transactions = transactions;
    }

    @Override
    public List<BankQuestion> listQuestions(String source) {
        return questions.list(source).stream().map(this::question).toList();
    }

    @Override
    public Optional<BankQuestion> findQuestion(String id) {
        return Optional.ofNullable(questions.selectById(id)).map(this::question);
    }

    @Override
    public boolean insertQuestion(BankQuestion question) {
        try {
            return questions.insert(entity(question)) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public boolean updateQuestion(BankQuestion question, long expectedVersion) {
        return questions.updateVersioned(entity(question), expectedVersion) == 1;
    }

    @Override
    public boolean deleteQuestion(String id, long expectedVersion) {
        return questions.deleteVersioned(id, expectedVersion) == 1;
    }

    @Override
    public Optional<BankGenerationTask> findTaskByRequestId(String requestId) {
        return Optional.ofNullable(tasks.findByRequestId(requestId))
                .map(this::task);
    }

    @Override
    public Optional<BankGenerationTask> findActiveTask(String source, String documentVersion) {
        String activeKey = BankQuestionKeys.activeTaskKey(source, documentVersion);
        return Optional.ofNullable(tasks.findByActiveKey(activeKey))
                .map(this::task);
    }

    @Override
    public BankGenerationTask createOrFindTask(BankGenerationTask task) {
        try {
            tasks.insert(entity(task));
            return task;
        } catch (DuplicateKeyException exception) {
            BankGenerationTaskEntity existing = tasks.findByRequestId(task.requestId());
            if (existing != null) {
                return task(existing);
            }
            if (tasks.findByActiveKey(BankQuestionKeys.activeTaskKey(
                    task.source(), task.documentVersion())) != null) {
                throw activeTaskConflict();
            }
            throw exception;
        }
    }

    @Override
    public Optional<BankGenerationTask> findTask(String id) {
        return Optional.ofNullable(tasks.selectById(id))
                .map(this::task);
    }

    @Override
    public List<BankGenerationTask> listTasks(String source) {
        return tasks.list(source).stream()
                .map(this::task).toList();
    }

    @Override
    public List<BankGenerationTask> unfinishedTasks() {
        return tasks.unfinished().stream()
                .map(this::task).toList();
    }

    @Override
    public boolean updateTask(BankGenerationTask task, long expectedVersion) {
        try {
            return tasks.updateVersioned(entity(task), expectedVersion) == 1;
        } catch (DuplicateKeyException exception) {
            if (isActive(task.status())) {
                throw activeTaskConflict();
            }
            throw exception;
        }
    }

    @Override
    public boolean heartbeat(String taskId, String attemptId, String workerId,
                             LocalDateTime heartbeatAt) {
        return tasks.heartbeat(taskId, attemptId, workerId, heartbeatAt) == 1;
    }

    @Override
    public boolean recoverStale(BankGenerationTask replacement, long expectedVersion,
                                String expectedAttemptId, LocalDateTime staleBefore) {
        try {
            return tasks.recoverStale(entity(replacement), expectedVersion,
                    expectedAttemptId, staleBefore) == 1;
        } catch (DuplicateKeyException exception) {
            if (isActive(replacement.status())) {
                throw activeTaskConflict();
            }
            throw exception;
        }
    }

    @Override
    public boolean saveGenerated(BankQuestion question, BankGenerationTask expectedTask) {
        // 任务进度与题目写入共用事务；重复题和失效 attempt 均不会消耗成功题量。
        return Boolean.TRUE.equals(transactions.execute(transaction -> {
            BankGenerationTask updated = expectedTask.progress(BankGenerationTask.Status.PROCESSING,
                    expectedTask.savedCount() + 1, expectedTask.skippedCount(),
                    expectedTask.attemptId(), null);
            if (expectedTask.status() != BankGenerationTask.Status.PROCESSING
                    || !updateTask(updated, expectedTask.version())) {
                return false;
            }
            if (!insertQuestion(question)) {
                transaction.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public void recordUsage(String sessionId, String questionId, int questionIndex) {
        transactions.executeWithoutResult(transaction -> {
            if (questions.insertUsage(sessionId, questionId, questionIndex) == 1) {
                questions.incrementUsage(questionId);
            }
        });
    }

    private BankQuestion question(BankQuestionEntity entity) {
        BankQuestion value = read(entity.getQuestionJson(), BankQuestion.class);
        return new BankQuestion(value.id(), value.generationTaskId(), value.source(),
                value.documentVersion(), value.skill(), value.difficulty(), value.category(),
                value.question(), value.topicSummary(), value.referenceAnswer(), value.keyPoints(),
                value.scoringRubric(), value.followUps(), value.evidence(),
                BankQuestion.Status.valueOf(entity.getStatus()), entity.getVersion(),
                entity.getUseCount(), entity.getLastUsedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private BankQuestionEntity entity(BankQuestion question) {
        BankQuestionEntity entity = new BankQuestionEntity();
        entity.setId(question.id());
        entity.setGenerationTaskId(question.generationTaskId());
        entity.setSource(question.source());
        entity.setDocumentVersion(question.documentVersion());
        entity.setContentKey(BankQuestionKeys.contentKey(question));
        entity.setStatus(question.status().name());
        entity.setQuestionJson(write(question));
        entity.setVersion(question.version());
        entity.setUseCount(question.useCount());
        entity.setLastUsedAt(question.lastUsedAt());
        entity.setCreatedAt(question.createdAt());
        entity.setUpdatedAt(question.updatedAt());
        return entity;
    }

    private BankGenerationTask task(BankGenerationTaskEntity entity) {
        BankGenerationTask value = read(entity.getTaskJson(), BankGenerationTask.class);
        return new BankGenerationTask(value.id(), value.requestId(), value.source(),
                value.documentVersion(), value.skill(), value.difficulty(), value.questionCount(),
                value.followUpCount(), BankGenerationTask.Status.valueOf(entity.getStatus()),
                value.savedCount(), value.skippedCount(), entity.getAttemptId(), entity.getWorkerId(),
                entity.getStartedAt(), entity.getLastHeartbeatAt(),
                entity.getRecoveryCount() == null ? 0 : entity.getRecoveryCount(),
                value.promptVersion(), value.modelName(), value.errorCode(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private BankGenerationTaskEntity entity(BankGenerationTask task) {
        BankGenerationTaskEntity entity = new BankGenerationTaskEntity();
        entity.setId(task.id());
        entity.setRequestId(task.requestId());
        entity.setActiveKey(isActive(task.status())
                ? BankQuestionKeys.activeTaskKey(task.source(), task.documentVersion()) : null);
        entity.setSource(task.source());
        entity.setStatus(task.status().name());
        entity.setTaskJson(write(task));
        entity.setAttemptId(task.attemptId());
        entity.setWorkerId(task.workerId());
        entity.setStartedAt(task.startedAt());
        entity.setLastHeartbeatAt(task.lastHeartbeatAt());
        entity.setRecoveryCount(task.recoveryCount());
        entity.setVersion(task.version());
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
        return entity;
    }

    private boolean isActive(BankGenerationTask.Status status) {
        return status == BankGenerationTask.Status.QUEUED
                || status == BankGenerationTask.Status.PROCESSING;
    }

    private InterviewSessionConflictException activeTaskConflict() {
        return new InterviewSessionConflictException("当前资料版本已有题目生成任务正在执行，请等待完成后再提交");
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题库数据序列化失败", exception);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题库数据读取失败", exception);
        }
    }
}
