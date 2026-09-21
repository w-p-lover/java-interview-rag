package com.javainterview.rag.interview.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class ResumeAnalysisResultService {

    public static final String LOCAL_MODEL = "LOCAL";
    private static final double FACT_CHANGE_THRESHOLD = 0.72d;

    private final ResumeCenterStore resumeCenterStore;
    private final ResumeAnalysisResultStore resultStore;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisResultService(
            ResumeCenterStore resumeCenterStore,
            ResumeAnalysisResultStore resultStore,
            ObjectMapper objectMapper) {
        this.resumeCenterStore = Objects.requireNonNull(resumeCenterStore, "简历中心存储不能为空");
        this.resultStore = Objects.requireNonNull(resultStore, "简历分析结果存储不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 序列化器不能为空");
    }

    public <T> T execute(
            String resumeVersionId,
            ResumeAnalysisType analysisType,
            Object fingerprintInput,
            String contractVersion,
            String modelName,
            Supplier<T> action) {
        Objects.requireNonNull(action, "简历分析动作不能为空");
        if (resumeVersionId == null || resumeVersionId.isBlank()) {
            return action.get();
        }

        String normalizedVersionId = requireVersion(resumeVersionId);
        Objects.requireNonNull(analysisType, "简历分析类型不能为空");
        String normalizedContract = ResumeRecord.text(contractVersion, 100, "简历分析契约版本");
        String normalizedModel = ResumeRecord.text(modelName, 100, "简历分析模型名称");
        String fingerprint = fingerprint(
                analysisType, fingerprintInput, normalizedContract, normalizedModel);

        T result = action.get();
        LocalDateTime now = LocalDateTime.now();
        resultStore.saveSuccessful(new ResumeAnalysisResult(
                UUID.randomUUID().toString(), normalizedVersionId, analysisType,
                fingerprint, normalizedContract, normalizedModel, writeJson(result), now, now));
        return result;
    }

    public List<ResultView> findAll(String resumeVersionId) {
        String normalizedVersionId = requireVersion(resumeVersionId);
        return resultStore.findAll(normalizedVersionId).stream().map(this::view).toList();
    }

    public ResultView findLatest(String resumeVersionId, ResumeAnalysisType analysisType) {
        String normalizedVersionId = requireVersion(resumeVersionId);
        Objects.requireNonNull(analysisType, "简历分析类型不能为空");
        return resultStore.findLatest(normalizedVersionId, analysisType)
                .map(this::view)
                .orElseThrow(() -> new NoSuchElementException("当前简历版本没有该类型的分析结果"));
    }

    public EvidenceAuditComparison compareEvidenceAudits(
            String baseVersionId,
            String targetVersionId) {
        ResumeVersion baseVersion = requireVersionEntity(baseVersionId);
        ResumeVersion targetVersion = requireVersionEntity(targetVersionId);
        if (baseVersion.id().equals(targetVersion.id())) {
            throw new IllegalArgumentException("请选择两个不同的简历版本");
        }
        if (!baseVersion.resumeId().equals(targetVersion.resumeId())) {
            throw new IllegalArgumentException("只能比较同一条简历记录的不同版本");
        }

        ResumeEvidenceAuditResult base = evidenceAudit(baseVersion.id(), "基准");
        ResumeEvidenceAuditResult target = evidenceAudit(targetVersion.id(), "目标");
        return compare(baseVersion.id(), targetVersion.id(),
                base.confirmedFacts(), target.confirmedFacts());
    }

    private String requireVersion(String resumeVersionId) {
        return requireVersionEntity(resumeVersionId).id();
    }

    private ResumeVersion requireVersionEntity(String resumeVersionId) {
        String normalized = ResumeRecord.text(resumeVersionId, 36, "简历版本 ID");
        return resumeCenterStore.findVersion(normalized)
                .orElseThrow(() -> new NoSuchElementException("简历版本不存在"));
    }

    private ResumeEvidenceAuditResult evidenceAudit(String resumeVersionId, String label) {
        ResumeAnalysisResult result = resultStore.findLatest(
                        resumeVersionId, ResumeAnalysisType.EVIDENCE_AUDIT)
                .orElseThrow(() -> new NoSuchElementException(
                        label + "简历版本没有证据审计结果"));
        try {
            return objectMapper.readValue(result.resultJson(), ResumeEvidenceAuditResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历证据审计结果反序列化失败", exception);
        }
    }

    private EvidenceAuditComparison compare(
            String baseVersionId,
            String targetVersionId,
            List<ResumeEvidenceAuditResult.ConfirmedFact> baseFacts,
            List<ResumeEvidenceAuditResult.ConfirmedFact> targetFacts) {
        boolean[] matchedBase = new boolean[baseFacts.size()];
        boolean[] matchedTarget = new boolean[targetFacts.size()];
        List<ResumeEvidenceAuditResult.ConfirmedFact> unchanged = new ArrayList<>();

        for (int baseIndex = 0; baseIndex < baseFacts.size(); baseIndex++) {
            for (int targetIndex = 0; targetIndex < targetFacts.size(); targetIndex++) {
                if (!matchedTarget[targetIndex]
                        && exactFact(baseFacts.get(baseIndex), targetFacts.get(targetIndex))) {
                    matchedBase[baseIndex] = true;
                    matchedTarget[targetIndex] = true;
                    unchanged.add(targetFacts.get(targetIndex));
                    break;
                }
            }
        }

        // 同类型事实只有在新旧文本足够接近时才判为变化，避免把独立事实强行配对。
        List<FactCandidate> candidates = new ArrayList<>();
        for (int baseIndex = 0; baseIndex < baseFacts.size(); baseIndex++) {
            if (matchedBase[baseIndex]) continue;
            for (int targetIndex = 0; targetIndex < targetFacts.size(); targetIndex++) {
                if (matchedTarget[targetIndex]) continue;
                ResumeEvidenceAuditResult.ConfirmedFact before = baseFacts.get(baseIndex);
                ResumeEvidenceAuditResult.ConfirmedFact after = targetFacts.get(targetIndex);
                if (before.type() != after.type()) continue;
                double score = factSimilarity(before, after);
                if (score >= FACT_CHANGE_THRESHOLD) {
                    candidates.add(new FactCandidate(baseIndex, targetIndex, score));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(FactCandidate::score).reversed()
                .thenComparingInt(FactCandidate::baseIndex)
                .thenComparingInt(FactCandidate::targetIndex));

        List<FactChange> changed = new ArrayList<>();
        for (FactCandidate candidate : candidates) {
            if (matchedBase[candidate.baseIndex()] || matchedTarget[candidate.targetIndex()]) continue;
            matchedBase[candidate.baseIndex()] = true;
            matchedTarget[candidate.targetIndex()] = true;
            changed.add(new FactChange(
                    baseFacts.get(candidate.baseIndex()),
                    targetFacts.get(candidate.targetIndex())));
        }

        List<ResumeEvidenceAuditResult.ConfirmedFact> removed = new ArrayList<>();
        for (int index = 0; index < baseFacts.size(); index++) {
            if (!matchedBase[index]) removed.add(baseFacts.get(index));
        }
        List<ResumeEvidenceAuditResult.ConfirmedFact> added = new ArrayList<>();
        for (int index = 0; index < targetFacts.size(); index++) {
            if (!matchedTarget[index]) added.add(targetFacts.get(index));
        }
        return new EvidenceAuditComparison(
                baseVersionId, targetVersionId,
                List.copyOf(unchanged), List.copyOf(changed),
                List.copyOf(added), List.copyOf(removed));
    }

    private boolean exactFact(
            ResumeEvidenceAuditResult.ConfirmedFact left,
            ResumeEvidenceAuditResult.ConfirmedFact right) {
        return left.type() == right.type()
                && normalizeFactText(left.statement()).equals(normalizeFactText(right.statement()))
                && normalizeFactText(left.evidence()).equals(normalizeFactText(right.evidence()));
    }

    private double factSimilarity(
            ResumeEvidenceAuditResult.ConfirmedFact before,
            ResumeEvidenceAuditResult.ConfirmedFact after) {
        double statementScore = textSimilarity(before.statement(), after.statement());
        double evidenceScore = textSimilarity(before.evidence(), after.evidence());
        return Math.max(statementScore, evidenceScore) * 0.7d
                + Math.min(statementScore, evidenceScore) * 0.3d;
    }

    private double textSimilarity(String left, String right) {
        String normalizedLeft = normalizeFactText(left);
        String normalizedRight = normalizeFactText(right);
        if (normalizedLeft.equals(normalizedRight)) return 1.0d;
        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());
        if (maxLength == 0) return 1.0d;
        return 1.0d - ((double) editDistance(normalizedLeft, normalizedRight) / maxLength);
    }

    private String normalizeFactText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{Z}\\s]+", "");
    }

    private int editDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) previous[index] = index;
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int substitution = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private ResultView view(ResumeAnalysisResult result) {
        try {
            return new ResultView(
                    result.id(), result.resumeVersionId(), result.analysisType(),
                    result.inputFingerprint(), result.contractVersion(), result.modelName(),
                    objectMapper.readTree(result.resultJson()), result.createdAt(), result.updatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历分析结果反序列化失败", exception);
        }
    }

    private String writeJson(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历分析结果序列化失败", exception);
        }
    }

    private String fingerprint(
            ResumeAnalysisType analysisType,
            Object input,
            String contractVersion,
            String modelName) {
        try {
            String canonicalInput = objectMapper.writeValueAsString(input);
            String source = analysisType.name() + "\n" + contractVersion + "\n"
                    + modelName + "\n" + canonicalInput;
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("简历分析输入无法生成指纹", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    public record ResultView(
            String id,
            String resumeVersionId,
            ResumeAnalysisType analysisType,
            String inputFingerprint,
            String contractVersion,
            String modelName,
            JsonNode result,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) { }

    public record EvidenceAuditComparison(
            String baseVersionId,
            String targetVersionId,
            List<ResumeEvidenceAuditResult.ConfirmedFact> unchangedFacts,
            List<FactChange> changedFacts,
            List<ResumeEvidenceAuditResult.ConfirmedFact> addedFacts,
            List<ResumeEvidenceAuditResult.ConfirmedFact> removedFacts
    ) { }

    public record FactChange(
            ResumeEvidenceAuditResult.ConfirmedFact before,
            ResumeEvidenceAuditResult.ConfirmedFact after
    ) { }

    private record FactCandidate(int baseIndex, int targetIndex, double score) { }
}
