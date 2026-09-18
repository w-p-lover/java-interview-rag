package com.javainterview.rag.interview.draft;

import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.interview.question.InterviewQuestionPlanOutcome;
import com.javainterview.rag.interview.question.InterviewQuestionPlanService;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class InterviewDraftService {

    private static final int MAX_CACHE_ENTRIES = 100;
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final InterviewPreparationService preparationService;
    private final InterviewQuestionPlanService questionPlanService;
    private final Clock clock;
    private final Object cacheLock = new Object();
    private final LinkedHashMap<String, DraftEntry> entriesById = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<String, String> draftIdByFingerprint = new HashMap<>();

    public InterviewDraftService(InterviewPreparationService preparationService,
                                 InterviewQuestionPlanService questionPlanService) {
        this(preparationService, questionPlanService, Clock.systemUTC());
    }

    InterviewDraftService(InterviewPreparationService preparationService,
                          InterviewQuestionPlanService questionPlanService,
                          Clock clock) {
        this.preparationService = java.util.Objects.requireNonNull(
                preparationService, "面试准备服务不能为空");
        this.questionPlanService = java.util.Objects.requireNonNull(
                questionPlanService, "面试组卷服务不能为空");
        this.clock = java.util.Objects.requireNonNull(clock, "时钟不能为空");
    }

    public InterviewDraftResult generate(String targetRole,
                                         String resumeText,
                                         String jobDescription,
                                         int questionCount,
                                         int maxQuestionCount,
                                         InterviewSkill skill,
                                         InterviewDifficulty difficulty,
                                         boolean regenerate) {
        questionPlanService.validateConfiguration(questionCount, maxQuestionCount, skill, difficulty);
        String normalizedTargetRole = normalize(targetRole);
        String normalizedResumeText = normalize(resumeText);
        String normalizedJobDescription = normalize(jobDescription);
        String fingerprint = fingerprint(
                normalizedTargetRole,
                normalizedResumeText,
                normalizedJobDescription,
                questionCount,
                maxQuestionCount,
                skill,
                difficulty);

        DraftEntry entry;
        boolean owner = false;
        synchronized (cacheLock) {
            removeExpiredEntries();
            entry = regenerate ? null : findByFingerprint(fingerprint);
            if (entry == null) {
                entry = new DraftEntry(UUID.randomUUID().toString(), fingerprint, clock.millis());
                entriesById.put(entry.draftId, entry);
                draftIdByFingerprint.put(fingerprint, entry.draftId);
                trimCache();
                owner = true;
            }
        }
        if (!owner) {
            return await(entry).withReused(true);
        }

        try {
            InterviewPreparationResult preparation = preparationService.prepare(
                    normalizedTargetRole, normalizedResumeText, normalizedJobDescription);
            InterviewQuestionPlanOutcome plan = questionPlanService.generatePlan(
                    preparation.targetRole(),
                    preparation.profile(),
                    questionCount,
                    maxQuestionCount,
                    skill,
                    difficulty,
                    preparation.promptVersion());
            InterviewDraftResult result = result(
                    entry.draftId,
                    questionCount,
                    preparation,
                    plan.generation(),
                    plan.failureCode(),
                    plan.failureMessage());
            entry.current = result;
            entry.completion.complete(null);
            return result;
        } catch (RuntimeException exception) {
            entry.completion.completeExceptionally(exception);
            removeFailedEntry(entry);
            throw exception;
        }
    }

    public InterviewDraftResult replaceQuestion(String draftId,
                                                int questionIndex,
                                                InterviewQuestionGenerationResult currentGeneration) {
        DraftEntry entry = findRequired(draftId);
        await(entry);
        synchronized (entry) {
            InterviewDraftResult current = entry.current;
            validateReplacementSnapshot(current, currentGeneration);
            InterviewQuestionGenerationResult updatedGeneration = questionPlanService.replaceQuestion(
                    current.preparation().targetRole(),
                    current.preparation().profile(),
                    currentGeneration,
                    questionIndex);
            InterviewDraftResult updated = result(
                    current.draftId(),
                    current.targetQuestionCount(),
                    current.preparation(),
                    updatedGeneration,
                    current.failureCode(),
                    current.failureMessage());
            entry.current = updated;
            return updated;
        }
    }

    private InterviewDraftResult result(String draftId,
                                        int targetQuestionCount,
                                        InterviewPreparationResult preparation,
                                        InterviewQuestionGenerationResult generation,
                                        String failureCode,
                                        String failureMessage) {
        List<InterviewQuestionSource> unavailableSources = generation.unavailableSources();
        int actualQuestionCount = generation.questions().size();
        InterviewDraftStatus status = actualQuestionCount == targetQuestionCount
                && unavailableSources.isEmpty()
                ? InterviewDraftStatus.COMPLETE : InterviewDraftStatus.PARTIAL;
        String resolvedFailureCode = failureCode;
        String resolvedFailureMessage = failureMessage;
        if (resolvedFailureCode == null
                && unavailableSources.contains(InterviewQuestionSource.KNOWLEDGE)) {
            resolvedFailureCode = "KNOWLEDGE_SEARCH_UNAVAILABLE";
            resolvedFailureMessage = "部分题目的知识检索暂不可用，题目已保留";
        }
        return new InterviewDraftResult(
                draftId,
                false,
                targetQuestionCount,
                actualQuestionCount,
                status,
                unavailableSources,
                resolvedFailureCode,
                resolvedFailureMessage,
                preparation,
                generation);
    }

    private void validateReplacementSnapshot(InterviewDraftResult current,
                                             InterviewQuestionGenerationResult supplied) {
        if (supplied == null) {
            throw new IllegalArgumentException("当前题目草稿不能为空");
        }
        var expected = current.generation().configuration();
        var actual = supplied.configuration();
        if (!current.preparation().targetRole().equals(supplied.targetRole())
                || !expected.strategyVersion().equals(actual.strategyVersion())
                || expected.skill() != actual.skill()
                || expected.difficulty() != actual.difficulty()
                || !expected.preparationPromptVersion().equals(actual.preparationPromptVersion())
                || !expected.questionPromptVersion().equals(actual.questionPromptVersion())
                || !expected.modelName().equals(actual.modelName())) {
            throw new IllegalArgumentException("当前题目草稿与原始配置不一致");
        }
    }

    private DraftEntry findRequired(String draftId) {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("草稿 ID 不能为空");
        }
        synchronized (cacheLock) {
            removeExpiredEntries();
            DraftEntry entry = entriesById.get(draftId.strip());
            if (entry == null) {
                throw new NoSuchElementException("面试题草稿不存在或已过期，请重新生成");
            }
            return entry;
        }
    }

    private DraftEntry findByFingerprint(String fingerprint) {
        String draftId = draftIdByFingerprint.get(fingerprint);
        return draftId == null ? null : entriesById.get(draftId);
    }

    private InterviewDraftResult await(DraftEntry entry) {
        try {
            entry.completion.join();
            return entry.current;
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private void removeFailedEntry(DraftEntry entry) {
        synchronized (cacheLock) {
            if (entriesById.get(entry.draftId) == entry) {
                entriesById.remove(entry.draftId);
            }
            draftIdByFingerprint.remove(entry.fingerprint, entry.draftId);
        }
    }

    private void removeExpiredEntries() {
        long expiresBefore = clock.millis() - CACHE_TTL.toMillis();
        List<DraftEntry> expired = entriesById.values().stream()
                .filter(entry -> entry.createdAtMillis < expiresBefore)
                .toList();
        expired.forEach(this::removeEntry);
    }

    private void trimCache() {
        while (entriesById.size() > MAX_CACHE_ENTRIES) {
            DraftEntry eldest = entriesById.values().iterator().next();
            removeEntry(eldest);
        }
    }

    private void removeEntry(DraftEntry entry) {
        entriesById.remove(entry.draftId);
        draftIdByFingerprint.remove(entry.fingerprint, entry.draftId);
    }

    private String fingerprint(String targetRole,
                               String resumeText,
                               String jobDescription,
                               int questionCount,
                               int maxQuestionCount,
                               InterviewSkill skill,
                               InterviewDifficulty difficulty) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<String> parts = new ArrayList<>();
            parts.add(targetRole);
            parts.add(resumeText);
            parts.add(jobDescription);
            parts.add(Integer.toString(questionCount));
            parts.add(Integer.toString(maxQuestionCount));
            parts.add(skill.name());
            parts.add(difficulty.name());
            parts.add(InterviewPreparationService.PROMPT_VERSION);
            parts.add(InterviewQuestionGenerationService.PROMPT_VERSION);
            parts.add(questionPlanService.modelName());
            parts.add(InterviewQuestionPlanService.STRATEGY_VERSION);
            parts.forEach(part -> updateDigest(digest, part));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private static final class DraftEntry {
        private final String draftId;
        private final String fingerprint;
        private final long createdAtMillis;
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private volatile InterviewDraftResult current;

        private DraftEntry(String draftId, String fingerprint, long createdAtMillis) {
            this.draftId = draftId;
            this.fingerprint = fingerprint;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
