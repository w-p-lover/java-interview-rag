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

public class KnowledgeReviewTaskService {
    private static final List<RagAnswerClaimEvidenceStatus> REVIEWABLE = List.of(
            RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT,
            RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING,
            RagAnswerClaimEvidenceStatus.CITATION_INVALID);

    private final KnowledgeReviewTaskStore store;

    public KnowledgeReviewTaskService(KnowledgeReviewTaskStore store) {
        this.store = Objects.requireNonNull(store, "知识复习任务存储不能为空");
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
