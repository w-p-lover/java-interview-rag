package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSessionMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InterviewQuestionPlanService {

    public static final String STRATEGY_VERSION = "deterministic-multi-source-v1";

    private final InterviewQuestionGenerationService generationService;
    private final FixedInterviewQuestionCatalog fixedQuestionCatalog;
    private final String modelName;

    public InterviewQuestionPlanService(InterviewQuestionGenerationService generationService,
                                        FixedInterviewQuestionCatalog fixedQuestionCatalog,
                                        String modelName) {
        this.generationService = Objects.requireNonNull(generationService, "面试出题服务不能为空");
        this.fixedQuestionCatalog = Objects.requireNonNull(
                fixedQuestionCatalog, "固定题目录不能为空");
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
                difficulty, preparationPromptVersion).generation();
    }

    public InterviewQuestionPlanOutcome generatePlan(String targetRole,
                                                     InterviewPreparationProfile profile,
                                                     int questionCount,
                                                     int maxQuestionCount,
                                                     InterviewSkill skill,
                                                     InterviewDifficulty difficulty,
                                                     String preparationPromptVersion) {
        validateConfiguration(questionCount, maxQuestionCount, skill, difficulty);
        if (profile == null) {
            throw new IllegalArgumentException("面试准备画像不能为空");
        }
        String normalizedTargetRole = requireTargetRole(targetRole);
        String normalizedPreparationPromptVersion = requireText(
                preparationPromptVersion, "准备画像 Prompt 版本不能为空");
        int fixedCount = fixedQuestionCount(questionCount);
        int generatedCount = questionCount - fixedCount;
        List<PlannedQuestion> fixedQuestions = fixedQuestions(skill, difficulty, fixedCount);
        try {
            InterviewQuestionGenerationResult generated = generationService.generate(
                    normalizedTargetRole, profile, Math.max(3, generatedCount));
            List<PlannedQuestion> generatedQuestions = generated.questions().stream()
                    .limit(generatedCount)
                    .map(this::fromGeneratedQuestion)
                    .toList();
            List<PlannedQuestion> plannedQuestions = interleave(fixedQuestions, generatedQuestions);
            return new InterviewQuestionPlanOutcome(
                    toGenerationResult(
                            generated.targetRole(),
                            generated.promptVersion(),
                            plannedQuestions,
                            questionCount,
                            maxQuestionCount,
                            skill,
                            difficulty,
                            normalizedPreparationPromptVersion,
                            generated.unavailableSources()),
                    null,
                    null);
        } catch (RuntimeException exception) {
            List<InterviewQuestionSource> unavailableSources = profile.focusAreas().stream()
                    .flatMap(area -> area.sources().stream())
                    .map(this::toQuestionSource)
                    .distinct()
                    .toList();
            return new InterviewQuestionPlanOutcome(
                    toGenerationResult(
                            normalizedTargetRole,
                            InterviewQuestionGenerationService.PROMPT_VERSION,
                            fixedQuestions,
                            fixedQuestions.size(),
                            maxQuestionCount,
                            skill,
                            difficulty,
                            normalizedPreparationPromptVersion,
                            unavailableSources),
                    "QUESTION_GENERATION_UNAVAILABLE",
                    "画像题暂时生成失败，已保留可用的 Skill 固定题");
        }
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
                    current.questions().get(index),
                    currentAllocation.primarySource(),
                    currentAllocation.supportingSources(),
                    false));
        }
        List<InterviewQuestionSource> unavailableSources = new ArrayList<>(current.unavailableSources());
        if (replacement.knowledgeUnavailable()
                && !unavailableSources.contains(InterviewQuestionSource.KNOWLEDGE)) {
            unavailableSources.add(InterviewQuestionSource.KNOWLEDGE);
        }
        return toGenerationResult(
                current.targetRole(),
                current.promptVersion(),
                updated,
                updated.size(),
                configuration.maxQuestionCount(),
                configuration.skill(),
                configuration.difficulty(),
                configuration.preparationPromptVersion(),
                unavailableSources);
    }

    public void validateConfiguration(int questionCount,
                                      int maxQuestionCount,
                                      InterviewSkill skill,
                                      InterviewDifficulty difficulty) {
        validateCounts(questionCount, maxQuestionCount);
        fixedQuestionCatalog.select(skill, difficulty, fixedQuestionCount(questionCount));
    }

    public String modelName() {
        return modelName;
    }

    private PlannedQuestion nextFixedQuestion(InterviewQuestionGenerationResult current,
                                              InterviewSkill skill,
                                              InterviewDifficulty difficulty) {
        List<String> existingQuestions = current.questions().stream()
                .map(InterviewQuestion::question)
                .toList();
        InterviewQuestion replacement = fixedQuestionCatalog.select(skill, difficulty, 3).stream()
                .filter(candidate -> !existingQuestions.contains(candidate.question()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前 Skill 没有可用的替换题"));
        return new PlannedQuestion(
                replacement,
                InterviewQuestionSource.SKILL_FIXED,
                List.of(InterviewQuestionSource.TARGET_ROLE),
                false);
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
                targetRole,
                profile,
                preferredSource,
                current.questions().stream().map(InterviewQuestion::question).toList());
        PlannedQuestion planned = fromGeneratedQuestion(replacement.questions().get(0));
        return new PlannedQuestion(
                planned.question(),
                planned.primarySource(),
                planned.supportingSources(),
                replacement.unavailableSources().contains(InterviewQuestionSource.KNOWLEDGE));
    }

    private List<PlannedQuestion> fixedQuestions(InterviewSkill skill,
                                                 InterviewDifficulty difficulty,
                                                 int fixedCount) {
        return fixedQuestionCatalog.select(skill, difficulty, fixedCount)
                .stream()
                .map(question -> new PlannedQuestion(
                        question,
                        InterviewQuestionSource.SKILL_FIXED,
                        List.of(InterviewQuestionSource.TARGET_ROLE),
                        false))
                .toList();
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
                STRATEGY_VERSION,
                InterviewSessionMode.STANDARD,
                skill,
                difficulty,
                initialQuestionCount,
                maxQuestionCount,
                sourceQuotas,
                allocations,
                preparationPromptVersion,
                questionPromptVersion,
                modelName);
        return new InterviewQuestionGenerationResult(
                targetRole,
                questionPromptVersion,
                plannedQuestions.stream().map(PlannedQuestion::question).toList(),
                configuration,
                unavailableSources);
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

    private List<PlannedQuestion> interleave(List<PlannedQuestion> fixed,
                                             List<PlannedQuestion> generated) {
        List<PlannedQuestion> result = new ArrayList<>(fixed.size() + generated.size());
        result.add(fixed.get(0));
        int generatedIndex = 0;
        while (generatedIndex < generated.size()) {
            if (fixed.size() > 1 && generatedIndex == 2) {
                result.add(fixed.get(1));
            }
            result.add(generated.get(generatedIndex++));
        }
        if (fixed.size() > 1 && !result.contains(fixed.get(1))) {
            result.add(fixed.get(1));
        }
        return List.copyOf(result);
    }

    private int fixedQuestionCount(int questionCount) {
        return questionCount >= 5 ? 2 : 1;
    }

    private void validateCounts(int questionCount, int maxQuestionCount) {
        if (questionCount < 3 || questionCount > 8) {
            throw new IllegalArgumentException("初始题数必须在 3 到 8 之间");
        }
        if (maxQuestionCount < questionCount || maxQuestionCount > 12) {
            throw new IllegalArgumentException("整场题数上限必须在初始题数到 12 之间");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    private String requireTargetRole(String targetRole) {
        String normalized = requireText(targetRole, "目标岗位不能为空");
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("目标岗位不能超过 100 个字符");
        }
        return normalized;
    }

    private record PlannedQuestion(
            InterviewQuestion question,
            InterviewQuestionSource primarySource,
            List<InterviewQuestionSource> supportingSources,
            boolean knowledgeUnavailable
    ) {
    }
}
