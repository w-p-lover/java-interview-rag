package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public class RagEvaluationCandidateService {
    private final RagEvaluationCandidateStore store;
    private final RagAnswerService answerService;

    public RagEvaluationCandidateService(RagEvaluationCandidateStore store, RagAnswerService answerService) {
        this.store = Objects.requireNonNull(store, "评估候选存储不能为空");
        this.answerService = Objects.requireNonNull(answerService, "RAG 回答服务不能为空");
    }

    public RagEvaluationCandidate create(Long feedbackId, String question, List<String> expectedSources,
                                          List<String> expectedAnswerPoints) {
        String normalizedQuestion = requireLength(question, "评估问题不能为空", 2_000);
        List<String> sources = normalizeList(expectedSources, 500);
        List<String> points = normalizeList(expectedAnswerPoints, 500);
        if (points.isEmpty()) throw new IllegalArgumentException("评估候选至少需要一个预期回答要点");
        String key = normalizedQuestion + "\u0000" + sources + "\u0000" + points;
        return store.createIfAbsent(new RagEvaluationCandidate(sha256(key), feedbackId, normalizedQuestion,
                sources, points, RagEvaluationCandidateStatus.PENDING, LocalDateTime.now(), null));
    }

    public List<RagEvaluationCandidate> findRecent(int limit, RagEvaluationCandidateStatus status) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("评估候选数量必须在 1 到 100 之间");
        return store.findRecent(limit, status);
    }

    public RagEvaluationCandidate review(String id, RagEvaluationCandidateStatus status) {
        if (status != RagEvaluationCandidateStatus.ACTIVE && status != RagEvaluationCandidateStatus.IGNORED) {
            throw new IllegalArgumentException("评估候选只能激活或忽略");
        }
        return store.review(requireLength(id, "评估候选 ID 不能为空", 128), status);
    }

    public RagEvaluationCandidateResult evaluate(String id) {
        RagEvaluationCandidate candidate = store.findById(requireLength(id, "评估候选 ID 不能为空", 128))
                .orElseThrow(() -> new IllegalArgumentException("评估候选不存在"));
        if (candidate.status() != RagEvaluationCandidateStatus.ACTIVE) {
            throw new IllegalArgumentException("只有已激活的评估候选可以运行");
        }
        RagAnswerResult result = answerService.answer(candidate.question());
        String answer = normalize(result.answer());
        List<String> matchedPoints = candidate.expectedAnswerPoints().stream()
                .filter(point -> answer.contains(normalize(point))).toList();
        boolean sourceMatched = candidate.expectedSources().isEmpty() || result.sources().stream()
                .map(source -> source.source()).anyMatch(candidate.expectedSources()::contains);
        return new RagEvaluationCandidateResult(candidate.id(), candidate.question(), result.grounded(),
                sourceMatched, candidate.expectedAnswerPoints(), matchedPoints,
                result.sources().stream().map(source -> source.source()).distinct().toList(),
                result.retrievalStrategy(), result.degraded(), result.degradationReason());
    }

    private List<String> normalizeList(List<String> values, int maxLength) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank())
                .peek(value -> { if (value.length() > maxLength) throw new IllegalArgumentException("评估文本过长"); })
                .distinct().toList();
    }

    private String requireLength(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(message + "不能超过 " + maxLength + " 个字符");
        return normalized;
    }

    private String normalize(String value) {
        StringBuilder result = new StringBuilder();
        value.codePoints().filter(Character::isLetterOrDigit).map(Character::toLowerCase).forEach(result::appendCodePoint);
        return result.toString();
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 不可用", exception); }
    }
}
