package com.javainterview.rag.knowledge.review;

import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import java.util.UUID;

public class KnowledgeReviewTaskService {
    private static final List<RagAnswerClaimEvidenceStatus> REVIEWABLE = List.of(
            RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT,
            RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING,
            RagAnswerClaimEvidenceStatus.CITATION_INVALID);

    private final KnowledgeReviewTaskStore store;
    private KnowledgeReviewAttemptStore attemptStore;
    private InterviewAnswerEvaluator evaluator;

    public KnowledgeReviewTaskService(KnowledgeReviewTaskStore store) {
        this.store = Objects.requireNonNull(store, "知识复习任务存储不能为空");
    }

    public KnowledgeReviewTaskService(KnowledgeReviewTaskStore store, KnowledgeReviewAttemptStore attemptStore, InterviewAnswerEvaluator evaluator) {
        this(store); this.attemptStore = attemptStore; this.evaluator = evaluator;
    }

    public KnowledgeReviewAttempt submitAttempt(String taskId, String answer) {
        return submitAttempt(taskId, answer, UUID.randomUUID().toString());
    }

    public KnowledgeReviewAttempt submitAttempt(String taskId, String answer, String idempotencyKey) {
        if (attemptStore == null || evaluator == null) throw new IllegalStateException("知识复习评价能力尚未配置");
        String normalizedTaskId = requireId(taskId);
        String normalizedKey = requireIdempotencyKey(idempotencyKey);
        Optional<KnowledgeReviewAttempt> existing = attemptStore.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            if (!existing.get().taskId().equals(normalizedTaskId)
                    || !existing.get().answer().equals(answer == null ? "" : answer.strip())) {
                throw new IllegalArgumentException("幂等键已用于其他复习请求");
            }
            return existing.get();
        }
        KnowledgeReviewTask task = find(null, null, null).stream().filter(item -> item.id().equals(normalizedTaskId)).findFirst().orElseThrow(() -> new java.util.NoSuchElementException("知识复习任务不存在"));
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("复习回答不能为空");
        InterviewPreparationProfile profile = new InterviewPreparationProfile(
                "知识复习用户", List.of("知识问答复习任务"), List.of(),
                List.of(new InterviewFocusArea("知识复习", "针对证据问题复述并纠正", List.of(InterviewFocusSource.TARGET_ROLE))));
        InterviewQuestion question = new InterviewQuestion(task.question(), "解释并纠正：" + task.claim(),
                List.of(InterviewFocusSource.TARGET_ROLE), task.question(), false, List.of());
        var assessment = evaluator.evaluateForReview(profile, question, answer.strip(), List.of());
        return attemptStore.create(normalizedKey, new KnowledgeReviewAttempt(UUID.randomUUID().toString(), task.id(), answer.strip(), assessment.evaluation(), java.time.LocalDateTime.now()));
    }

    public List<KnowledgeReviewAttempt> findAttempts(String taskId) {
        if (attemptStore == null) throw new IllegalStateException("知识复习回答能力尚未配置");
        return attemptStore.findByTaskId(requireId(taskId));
    }

    public KnowledgeReviewTask create(String question, String claim,
                                      RagAnswerClaimEvidenceStatus evidenceStatus,
                                      List<Integer> sourceNumbers) {
        String normalizedQuestion = requireLength(question, "知识问题不能为空", 2_000);
        String normalizedClaim = requireLength(claim, "待复习断言不能为空", 2_000);
        if (!REVIEWABLE.contains(evidenceStatus)) {
            throw new IllegalArgumentException("只有证据不足、证据冲突或引用无效可以加入复习");
        }
        List<Integer> normalizedSources = sourceNumbers == null ? List.of() : sourceNumbers.stream()
                .filter(Objects::nonNull).distinct().sorted().toList();
        if (normalizedSources.stream().anyMatch(number -> number < 1)) {
            throw new IllegalArgumentException("引用资料编号必须从 1 开始");
        }
        String key = normalizedQuestion + "\u0000" + normalizedClaim + "\u0000"
                + evidenceStatus.name() + "\u0000" + normalizedSources;
        KnowledgeReviewTask task = new KnowledgeReviewTask(
                sha256(key), normalizedQuestion, normalizedClaim, evidenceStatus,
                normalizedSources, KnowledgeReviewTaskStatus.PENDING, null,
                LocalDateTime.now(), null);
        return store.createIfAbsent(task);
    }

    public List<KnowledgeReviewTask> find(String status, LocalDate plannedDateFrom,
                                          LocalDate plannedDateTo) {
        if (plannedDateFrom != null && plannedDateTo != null && plannedDateTo.isBefore(plannedDateFrom)) {
            throw new IllegalArgumentException("计划结束日期不能早于开始日期");
        }
        return store.find(parseStatus(status), plannedDateFrom, plannedDateTo);
    }

    public KnowledgeReviewTask complete(String taskId) {
        return store.complete(requireId(taskId), LocalDateTime.now())
                .orElseThrow(() -> new java.util.NoSuchElementException("知识复习任务不存在"));
    }

    public KnowledgeReviewTask schedule(String taskId, LocalDate plannedReviewDate) {
        return store.schedule(requireId(taskId), plannedReviewDate)
                .orElseThrow(() -> new java.util.NoSuchElementException("知识复习任务不存在"));
    }

    private KnowledgeReviewTaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return KnowledgeReviewTaskStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("复习任务状态仅支持 PENDING 或 COMPLETED");
        }
    }

    private String requireId(String taskId) {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("知识复习任务 ID 不能为空");
        return taskId.strip();
    }

    private String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("复习回答幂等键不能为空");
        String normalized = key.strip();
        if (normalized.length() > 128) throw new IllegalArgumentException("复习回答幂等键不能超过 128 个字符");
        return normalized;
    }

    private String requireLength(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(message + "不能超过 " + maxLength + " 个字符");
        return normalized;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
