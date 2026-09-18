package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSessionMode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class InterviewQuestionPlanService {

    public static final String STRATEGY_VERSION = "interview-guide-multi-source-v2";

    private final InterviewQuestionGenerationService generationService;
    private final FixedInterviewQuestionCatalog fixedQuestionCatalog;
    private final String modelName;

    public InterviewQuestionPlanService(InterviewQuestionGenerationService generationService,
                                        FixedInterviewQuestionCatalog fixedQuestionCatalog,
                                        String modelName) {
        this.generationService = Objects.requireNonNull(generationService, "面试出题服务不能为空");
        this.fixedQuestionCatalog = Objects.requireNonNull(fixedQuestionCatalog, "固定题目录不能为空");
        this.modelName = requireText(modelName, "模型名称不能为空");
    }

    public InterviewQuestionGenerationResult generate(String targetRole,
                                                       InterviewPreparationProfile profile,
                                                       int questionCount,
                                                       int maxQuestionCount,
                                                       InterviewSkill skill,
                                                       InterviewDifficulty difficulty,
                                                       String preparationPromptVersion) {
        return generatePlan(targetRole, profile, questionCount, maxQuestionCount, skill,
                difficulty, preparationPromptVersion, List.of()).generation();
    }

    public InterviewQuestionPlanOutcome generatePlan(String targetRole,
                                                     InterviewPreparationProfile profile,
                                                     int questionCount,
                                                     int maxQuestionCount,
                                                     InterviewSkill skill,
                                                     InterviewDifficulty difficulty,
                                                     String preparationPromptVersion) {
        return generatePlan(targetRole, profile, questionCount, maxQuestionCount, skill,
                difficulty, preparationPromptVersion, List.of());
    }

    public InterviewQuestionPlanOutcome generatePlan(String targetRole,
                                                     InterviewPreparationProfile profile,
                                                     int questionCount,
                                                     int maxQuestionCount,
                                                     InterviewSkill skill,
                                                     InterviewDifficulty difficulty,
                                                     String preparationPromptVersion,
                                                     List<HistoricalInterviewQuestion> historicalQuestions) {
        validateConfiguration(questionCount, maxQuestionCount, skill, difficulty);
        if (profile == null) {
            throw new IllegalArgumentException("面试准备画像不能为空");
        }
        String normalizedTargetRole = requireTargetRole(targetRole);
        String normalizedPreparationPromptVersion = requireText(
                preparationPromptVersion, "准备画像 Prompt 版本不能为空");
        List<HistoricalInterviewQuestion> history = historicalQuestions == null
                ? List.of() : List.copyOf(historicalQuestions);

        InterviewQuestionSourceGenerationResult generated = generationService.generateBySources(
                normalizedTargetRole, profile, questionCount, history);
        List<PlannedQuestion> planned = deduplicate(generated.questions().stream()
                .map(this::fromGeneratedQuestion)
                .toList(), history);
        int missingCount = questionCount - planned.size();
        if (missingCount > 0) {
            List<String> excluded = new ArrayList<>();
            history.stream().map(HistoricalInterviewQuestion::question).forEach(excluded::add);
            planned.stream().map(item -> item.question().question()).forEach(excluded::add);
            fixedQuestionCatalog.fallback(skill, difficulty, missingCount, excluded).stream()
                    .map(question -> new PlannedQuestion(
                            question,
                            InterviewQuestionSource.SKILL_FIXED,
                            List.of(InterviewQuestionSource.TARGET_ROLE),
                            false))
                    .forEach(planned::add);
        }
        List<InterviewQuestionSource> unavailableSources = generated.unavailableSources();
        String failureCode = unavailableSources.stream()
                .anyMatch(source -> source != InterviewQuestionSource.KNOWLEDGE)
                ? "QUESTION_SOURCE_DEGRADED" : null;
        String failureMessage = failureCode == null ? null
                : "部分出题来源不可用，已按 Skill 方向和固定题补足题量";
        return new InterviewQuestionPlanOutcome(
                toGenerationResult(
                        normalizedTargetRole,
                        generated.promptVersion(),
                        planned,
                        questionCount,
                        maxQuestionCount,
                        skill,
                        difficulty,
                        normalizedPreparationPromptVersion,
                        unavailableSources),
                failureCode,
                failureMessage);
    }

    public InterviewQuestionGenerationResult replaceQuestion(
            String targetRole,
            InterviewPreparationProfile profile,
            InterviewQuestionGenerationResult current,
            int questionIndex) {
        Objects.requireNonNull(profile, "面试准备画像不能为空");
        Objects.requireNonNull(current, "当前题目草稿不能为空");
        if (questionIndex < 0 || questionIndex >= current.questions().size()) {
            throw new IllegalArgumentException("替换题号超出当前草稿范围");
        }
        InterviewConfigurationSnapshot configuration = current.configuration();
        validateDraftConfiguration(current);
        InterviewQuestionAllocation allocation = configuration.questionAllocations().get(questionIndex);
        PlannedQuestion replacement = allocation.primarySource() == InterviewQuestionSource.SKILL_FIXED
                ? nextFixedQuestion(current, configuration.skill(), configuration.difficulty())
                : nextGeneratedQuestion(targetRole, profile, current, allocation.primarySource());
        List<PlannedQuestion> updated = new ArrayList<>(current.questions().size());
        for (int index = 0; index < current.questions().size(); index++) {
            if (index == questionIndex) {
                updated.add(replacement);
                continue;
            }
            InterviewQuestionAllocation currentAllocation = configuration.questionAllocations().get(index);
            updated.add(new PlannedQuestion(
                    current.questions().get(index), currentAllocation.primarySource(),
                    currentAllocation.supportingSources(), false));
        }
        List<InterviewQuestionSource> unavailableSources = new ArrayList<>(current.unavailableSources());
        if (replacement.knowledgeUnavailable()
                && !unavailableSources.contains(InterviewQuestionSource.KNOWLEDGE)) {
            unavailableSources.add(InterviewQuestionSource.KNOWLEDGE);
        }
        return toGenerationResult(
                current.targetRole(), current.promptVersion(), updated, updated.size(),
                configuration.maxQuestionCount(), configuration.skill(), configuration.difficulty(),
                configuration.preparationPromptVersion(), unavailableSources);
    }

    public void validateConfiguration(int questionCount,
                                      int maxQuestionCount,
                                      InterviewSkill skill,
                                      InterviewDifficulty difficulty) {
        validateCounts(questionCount, maxQuestionCount);
        fixedQuestionCatalog.select(skill, difficulty, 1);
    }

    public String modelName() {
        return modelName;
    }

    private List<PlannedQuestion> deduplicate(List<PlannedQuestion> questions,
                                              List<HistoricalInterviewQuestion> history) {
        LinkedHashSet<String> excluded = new LinkedHashSet<>();
        history.stream().map(HistoricalInterviewQuestion::question)
                .map(this::normalizeQuestion).forEach(excluded::add);
        Map<String, PlannedQuestion> unique = new LinkedHashMap<>();
        for (PlannedQuestion question : questions) {
            String normalized = normalizeQuestion(question.question().question());
            if (!excluded.contains(normalized)) {
                unique.putIfAbsent(normalized, question);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private PlannedQuestion nextFixedQuestion(InterviewQuestionGenerationResult current,
                                              InterviewSkill skill,
                                              InterviewDifficulty difficulty) {
        List<String> existingQuestions = current.questions().stream().map(InterviewQuestion::question).toList();
        InterviewQuestion replacement = fixedQuestionCatalog.fallback(
                skill, difficulty, 1, existingQuestions).get(0);
        return new PlannedQuestion(replacement, InterviewQuestionSource.SKILL_FIXED,
                List.of(InterviewQuestionSource.TARGET_ROLE), false);
    }

    private PlannedQuestion nextGeneratedQuestion(String targetRole,
                                                  InterviewPreparationProfile profile,
                                                  InterviewQuestionGenerationResult current,
                                                  InterviewQuestionSource primarySource) {
        InterviewFocusSource preferredSource = switch (primarySource) {
            case RESUME -> InterviewFocusSource.RESUME;
            case JOB_DESCRIPTION -> InterviewFocusSource.JOB_DESCRIPTION;
            default -> InterviewFocusSource.TARGET_ROLE;
        };
        InterviewQuestionGenerationResult replacement = generationService.generateReplacement(
                targetRole, profile, preferredSource,
                current.questions().stream().map(InterviewQuestion::question).toList());
        PlannedQuestion planned = fromGeneratedQuestion(replacement.questions().get(0));
        return new PlannedQuestion(
                planned.question(), planned.primarySource(), planned.supportingSources(),
                replacement.unavailableSources().contains(InterviewQuestionSource.KNOWLEDGE));
    }

    private InterviewQuestionGenerationResult toGenerationResult(
            String targetRole,
            String questionPromptVersion,
            List<PlannedQuestion> plannedQuestions,
            int initialQuestionCount,
            int maxQuestionCount,
            InterviewSkill skill,
            InterviewDifficulty difficulty,
            String preparationPromptVersion,
            List<InterviewQuestionSource> unavailableSources) {
        List<InterviewQuestionAllocation> allocations = new ArrayList<>();
        Map<InterviewQuestionSource, Integer> sourceQuotas = new EnumMap<>(InterviewQuestionSource.class);
        for (int index = 0; index < plannedQuestions.size(); index++) {
            PlannedQuestion planned = plannedQuestions.get(index);
            allocations.add(new InterviewQuestionAllocation(
                    index, planned.primarySource(), planned.supportingSources()));
            sourceQuotas.merge(planned.primarySource(), 1, Integer::sum);
        }
        InterviewConfigurationSnapshot configuration = new InterviewConfigurationSnapshot(
                STRATEGY_VERSION, InterviewSessionMode.STANDARD, skill, difficulty,
                initialQuestionCount, maxQuestionCount, sourceQuotas, allocations,
                preparationPromptVersion, questionPromptVersion, modelName);
        return new InterviewQuestionGenerationResult(
                targetRole, questionPromptVersion,
                plannedQuestions.stream().map(PlannedQuestion::question).toList(),
                configuration, unavailableSources);
    }

    private void validateDraftConfiguration(InterviewQuestionGenerationResult current) {
        InterviewConfigurationSnapshot configuration = current.configuration();
        if (!STRATEGY_VERSION.equals(configuration.strategyVersion())) {
            throw new IllegalArgumentException("当前草稿不支持单题替换");
        }
        if (configuration.questionAllocations().size() != current.questions().size()) {
            throw new IllegalArgumentException("当前草稿题目与来源分配不一致");
        }
    }

    private PlannedQuestion fromGeneratedQuestion(InterviewQuestion question) {
        LinkedHashSet<InterviewQuestionSource> sources = new LinkedHashSet<>();
        question.basisSources().stream().map(this::toQuestionSource).forEach(sources::add);
        if (question.knowledgeGrounded()) {
            sources.add(InterviewQuestionSource.KNOWLEDGE);
        }
        InterviewQuestionSource primarySource = sources.contains(InterviewQuestionSource.RESUME)
                ? InterviewQuestionSource.RESUME
                : sources.contains(InterviewQuestionSource.JOB_DESCRIPTION)
                ? InterviewQuestionSource.JOB_DESCRIPTION
                : InterviewQuestionSource.TARGET_ROLE;
        return new PlannedQuestion(question, primarySource, List.copyOf(sources), false);
    }

    private InterviewQuestionSource toQuestionSource(InterviewFocusSource source) {
        return switch (source) {
            case TARGET_ROLE -> InterviewQuestionSource.TARGET_ROLE;
            case RESUME -> InterviewQuestionSource.RESUME;
            case JOB_DESCRIPTION -> InterviewQuestionSource.JOB_DESCRIPTION;
        };
    }

    private String normalizeQuestion(String question) {
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]+", "");
        return normalized.isBlank() ? question.strip().toLowerCase(Locale.ROOT) : normalized;
    }

    private void validateCounts(int questionCount, int maxQuestionCount) {
        if (questionCount < 3 || questionCount > 8) {
            throw new IllegalArgumentException("初始题数必须在 3 到 8 之间");
        }
        if (maxQuestionCount < questionCount || maxQuestionCount > 12) {
            throw new IllegalArgumentException("整场题数上限必须在初始题数到 12 之间");
        }
    }

    private String requireTargetRole(String targetRole) {
        String normalized = requireText(targetRole, "目标岗位不能为空");
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("目标岗位不能超过 100 个字符");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    private record PlannedQuestion(
            InterviewQuestion question,
            InterviewQuestionSource primarySource,
            List<InterviewQuestionSource> supportingSources,
            boolean knowledgeUnavailable
    ) {
    }
}
