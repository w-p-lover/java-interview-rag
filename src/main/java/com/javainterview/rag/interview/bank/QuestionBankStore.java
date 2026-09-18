package com.javainterview.rag.interview.bank;

import java.util.List;
import java.util.Optional;

public interface QuestionBankStore {
    List<BankQuestion> listQuestions(String source);
    Optional<BankQuestion> findQuestion(String id);
    boolean insertQuestion(BankQuestion question);
    boolean updateQuestion(BankQuestion question, long expectedVersion);
    boolean deleteQuestion(String id, long expectedVersion);
    Optional<BankGenerationTask> findTaskByRequestId(String requestId);
    Optional<BankGenerationTask> findActiveTask(String source, String documentVersion);
    BankGenerationTask createOrFindTask(BankGenerationTask task);
    Optional<BankGenerationTask> findTask(String id);
    List<BankGenerationTask> listTasks(String source);
    List<BankGenerationTask> unfinishedTasks();
    boolean updateTask(BankGenerationTask task, long expectedVersion);
    boolean saveGenerated(BankQuestion question, BankGenerationTask expectedTask);
    void recordUsage(String sessionId, String questionId, int questionIndex);
}
