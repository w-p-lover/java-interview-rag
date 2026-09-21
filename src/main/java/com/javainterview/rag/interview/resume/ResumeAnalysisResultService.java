package com.javainterview.rag.interview.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class ResumeAnalysisResultService {

    public static final String LOCAL_MODEL = "LOCAL";

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

    private String requireVersion(String resumeVersionId) {
        String normalized = ResumeRecord.text(resumeVersionId, 36, "简历版本 ID");
        resumeCenterStore.findVersion(normalized)
                .orElseThrow(() -> new NoSuchElementException("简历版本不存在"));
        return normalized;
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
}
