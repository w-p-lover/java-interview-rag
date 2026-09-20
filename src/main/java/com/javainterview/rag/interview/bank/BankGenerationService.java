package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.question.InterviewSkillTrainingCatalog;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class BankGenerationService {
    private final QuestionBankStore store;
    private final BankSources sources;
    private final BankQuestionGenerator generator;
    private final Executor executor;
    private final String modelName;
    private final Set<String> scheduled = ConcurrentHashMap.newKeySet();
    private final InterviewSkillTrainingCatalog trainingCatalog = new InterviewSkillTrainingCatalog();

    public BankGenerationService(QuestionBankStore store, BankSources sources,
                                  BankQuestionGenerator generator, Executor executor,
                                  String modelName) {
        this.store = store;
        this.sources = sources;
        this.generator = generator;
        this.executor = executor;
        this.modelName = modelName;
    }

    @PostConstruct
    public void recoverInterrupted() {
        // 不在应用启动时重放付费生成；成功题目保留，用户可显式恢复缺口。
        store.unfinishedTasks().forEach(task -> store.updateTask(task.progress(
                BankGenerationTask.Status.FAILED, task.savedCount(), task.skippedCount(),
                task.attemptId(), "APPLICATION_RESTARTED"), task.version()));
    }

    public BankGenerationTask submit(String requestId, String source, InterviewSkill skill,
                                      InterviewDifficulty difficulty, int questionCount,
                                      int followUpCount) {
        requestId = BankQuestion.text(requestId, 100, "请求 ID");
        source = BankQuestion.text(source, 500, "资料来源");
        if (skill == null || skill == InterviewSkill.GENERAL || difficulty == null) {
            throw new IllegalArgumentException("请选择具体 Skill 和难度");
        }
        if (questionCount < 1 || questionCount > 8 || followUpCount < 0 || followUpCount > 2) {
            throw new IllegalArgumentException("生成题数必须在 1 到 8 之间，固定追问数在 0 到 2 之间");
        }
        var snapshot = sources.currentSnapshot(source);
        LocalDateTime now = LocalDateTime.now();
        BankGenerationTask requested = new BankGenerationTask(UUID.randomUUID().toString(),
                requestId, source, snapshot.document().version(), skill, difficulty,
                questionCount, followUpCount, BankGenerationTask.Status.QUEUED, 0, 0, null,
                BankQuestionGenerator.PROMPT_VERSION, modelName, null, 0, now, now);
        BankGenerationTask task = store.findTaskByRequestId(requestId).orElse(null);
        if (task == null) {
            store.findActiveTask(requested.source(), requested.documentVersion()).ifPresent(active -> {
                throw new InterviewSessionConflictException(
                        "当前资料版本已有题目生成任务正在执行，请等待完成后再提交");
            });
            task = store.createOrFindTask(requested);
        }
        if (!sameInput(task, requested)) {
            throw new InterviewSessionConflictException("该请求 ID 已用于不同生成配置，请刷新后重新提交");
        }
        if (task.status() == BankGenerationTask.Status.QUEUED) {
            schedule(task);
        }
        return find(task.id());
    }

    public BankGenerationTask find(String id) {
        return store.findTask(BankQuestion.text(id, 36, "任务 ID"))
                .orElseThrow(() -> new NoSuchElementException("题库生成任务不存在"));
    }

    public BankGenerationTask resume(String id) {
        BankGenerationTask task = find(id);
        if (task.status() != BankGenerationTask.Status.FAILED) {
            return task;
        }
        if (!sources.isCurrent(task.source(), task.documentVersion())) {
            throw new InterviewSessionConflictException("资料已更新或删除，不能恢复旧版本任务");
        }
        if (!modelName.equals(task.modelName())
                || !BankQuestionGenerator.PROMPT_VERSION.equals(task.promptVersion())) {
            throw new InterviewSessionConflictException("生成契约已变化，请创建新的生成任务");
        }
        store.findActiveTask(task.source(), task.documentVersion())
                .filter(active -> !active.id().equals(task.id()))
                .ifPresent(active -> {
                    throw new InterviewSessionConflictException(
                            "当前资料版本已有题目生成任务正在执行，请等待完成后再恢复");
                });
        BankGenerationTask queued = task.progress(BankGenerationTask.Status.QUEUED,
                task.savedCount(), task.skippedCount(), null, null);
        if (store.updateTask(queued, task.version())) {
            schedule(queued);
        }
        return find(id);
    }

    private boolean sameInput(BankGenerationTask left, BankGenerationTask right) {
        return left.source().equals(right.source()) && left.documentVersion().equals(right.documentVersion())
                && left.skill() == right.skill() && left.difficulty() == right.difficulty()
                && left.questionCount() == right.questionCount() && left.followUpCount() == right.followUpCount()
                && left.promptVersion().equals(right.promptVersion()) && left.modelName().equals(right.modelName());
    }

    private void schedule(BankGenerationTask task) {
        if (!scheduled.add(task.id())) {
            return;
        }
        try {
            executor.execute(() -> execute(task.id()));
        } catch (RuntimeException exception) {
            scheduled.remove(task.id());
            store.updateTask(task.progress(BankGenerationTask.Status.FAILED,
                    task.savedCount(), task.skippedCount(), null, "QUEUE_FULL"), task.version());
        }
    }

    private void execute(String id) {
        String attempt = UUID.randomUUID().toString();
        try {
            BankGenerationTask queued = find(id);
            if (queued.status() != BankGenerationTask.Status.QUEUED) {
                return;
            }
            BankGenerationTask processing = queued.progress(BankGenerationTask.Status.PROCESSING,
                    queued.savedCount(), queued.skippedCount(), attempt, null);
            if (!store.updateTask(processing, queued.version())) {
                return;
            }
            int remaining = processing.questionCount() - processing.savedCount();
            if (remaining <= 0) {
                complete(id, attempt, 0);
                return;
            }
            List<MarkdownSearchResult> context = sources.evidence(processing.source(), processing.documentVersion());
            List<BankQuestion> existing = store.listQuestions(processing.source()).stream()
                    .filter(question -> question.documentVersion().equals(processing.documentVersion())
                            && question.difficulty() == processing.difficulty())
                    .toList();
            List<String> existingQuestions = existing.stream().map(BankQuestion::question).toList();
            Map<String, Integer> remainingQuotas = new LinkedHashMap<>(trainingCatalog.plan(
                    processing.skill(), processing.questionCount(), null).quotaMap());
            existing.stream()
                    .filter(question -> processing.id().equals(question.generationTaskId()))
                    .forEach(question -> remainingQuotas.computeIfPresent(
                            question.category(), (category, count) -> count - 1));
            if (remainingQuotas.values().stream().anyMatch(count -> count < 0)) {
                throw new IllegalStateException("已保存题目的分类数量超过生成配额");
            }
            remainingQuotas.entrySet().removeIf(entry -> entry.getValue() == 0);
            if (remainingQuotas.values().stream().mapToInt(Integer::intValue).sum() != remaining) {
                throw new IllegalStateException("已保存题目的分类与生成进度不一致");
            }
            Map<String, Integer> generationQuotas = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(remainingQuotas));
            var generated = generator.generate(
                    processing, context, generationQuotas, existingQuestions);
            if (generated == null || generated.questions() == null || generated.questions().isEmpty()) {
                throw new IllegalStateException("模型未返回题库题目");
            }
            Set<String> seen = new HashSet<>();
            existingQuestions.stream().map(BankQuestionKeys::normalize).forEach(seen::add);
            int skipped = 0;
            for (var draft : generated.questions().stream().limit(remaining).toList()) {
                BankGenerationTask current = find(id);
                if (current.status() != BankGenerationTask.Status.PROCESSING
                        || !attempt.equals(current.attemptId())) {
                    return;
                }
                if (!sources.isCurrent(current.source(), current.documentVersion())) {
                    throw new InterviewSessionConflictException("资料在生成期间已变化");
                }
                BankQuestion question;
                try {
                    String category = draft.category() == null ? "" : draft.category().strip();
                    if (remainingQuotas.getOrDefault(category, 0) < 1) {
                        skipped++;
                        continue;
                    }
                    question = validateDraft(current, draft, context);
                    if (!seen.add(BankQuestionKeys.normalize(question.question()))) {
                        skipped++;
                        continue;
                    }
                } catch (IllegalArgumentException | NullPointerException exception) {
                    skipped++;
                    continue;
                }
                if (!store.saveGenerated(question, current)) {
                    skipped++;
                } else {
                    remainingQuotas.computeIfPresent(
                            question.category(), (category, count) -> count - 1);
                }
            }
            complete(id, attempt, skipped);
        } catch (RuntimeException exception) {
            BankGenerationTask current = find(id);
            if (attempt.equals(current.attemptId()) && current.status() == BankGenerationTask.Status.PROCESSING) {
                String code = "MODEL_TIMEOUT".equals(exception.getMessage())
                        ? "MODEL_TIMEOUT"
                        : "MODEL_CALL_FAILED".equals(exception.getMessage())
                        ? "MODEL_CALL_FAILED"
                        : exception instanceof InterviewSessionConflictException
                        ? "SOURCE_VERSION_CHANGED" : "GENERATION_FAILED";
                store.updateTask(current.progress(BankGenerationTask.Status.FAILED,
                        current.savedCount(), current.skippedCount(), attempt, code), current.version());
            }
        } finally {
            scheduled.remove(id);
        }
    }

    private BankQuestion validateDraft(BankGenerationTask task,
                                       BankQuestionGenerator.GeneratedQuestion draft,
                                       List<MarkdownSearchResult> context) {
        Objects.requireNonNull(draft, "题目不能为空");
        if (draft.evidenceChunkOrders() == null || draft.evidenceChunkOrders().isEmpty()
                || draft.evidenceChunkOrders().size() > 3 || draft.evidenceQuote() == null
                || draft.evidenceQuote().isBlank()) {
            throw new IllegalArgumentException("题目没有可核验的资料证据");
        }
        List<MarkdownSearchResult> evidence = draft.evidenceChunkOrders().stream().distinct()
                .map(order -> context.stream().filter(item -> item.chunkOrder() == order)
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("资料编号无效")))
                .toList();
        if (evidence.stream().noneMatch(item -> item.content().contains(draft.evidenceQuote().strip()))) {
            throw new IllegalArgumentException("模型引用不在对应知识资料中");
        }
        if (draft.followUps() == null || draft.followUps().size() != task.followUpCount()) {
            throw new IllegalArgumentException("固定追问数量与生成配置不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        return new BankQuestion(UUID.randomUUID().toString(), task.id(), task.source(), task.documentVersion(),
                task.skill(), task.difficulty(), draft.category(), draft.question(), draft.topicSummary(),
                draft.referenceAnswer(), draft.keyPoints(), draft.scoringRubric(), draft.followUps().stream()
                        .map(item -> new BankQuestion.FollowUp(item.question(), item.referenceAnswer(),
                                item.keyPoints(), item.scoringRubric())).toList(), evidence,
                BankQuestion.Status.DRAFT, 0, 0, null, now, now);
    }

    private void complete(String id, String attempt, int skipped) {
        BankGenerationTask task = find(id);
        if (!attempt.equals(task.attemptId()) || task.status() != BankGenerationTask.Status.PROCESSING) {
            return;
        }
        boolean complete = task.savedCount() >= task.questionCount();
        store.updateTask(task.progress(complete ? BankGenerationTask.Status.COMPLETED
                        : BankGenerationTask.Status.FAILED, task.savedCount(), task.skippedCount() + skipped,
                attempt, complete ? null : "INCOMPLETE_GENERATION"), task.version());
    }
}
