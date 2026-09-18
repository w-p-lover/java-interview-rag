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
        validateCounts(questionCount, maxQuestionCount);
        String normalizedPreparationPromptVersion = requireText(
                preparationPromptVersion, "准备画像 Prompt 版本不能为空");
        int fixedCount = questionCount >= 5 ? 2 : 1;
        int generatedCount = questionCount - fixedCount;
        List<PlannedQuestion> fixedQuestions = fixedQuestionCatalog
                .select(skill, difficulty, fixedCount)
                .stream()
                .map(question -> new PlannedQuestion(
                        question,
                        InterviewQuestionSource.SKILL_FIXED,
                        List.of(InterviewQuestionSource.TARGET_ROLE)))
                .toList();
        InterviewQuestionGenerationResult generated = generationService.generate(
                targetRole, profile, Math.max(3, generatedCount));
        List<PlannedQuestion> generatedQuestions = generated.questions().stream()
                .limit(generatedCount)
                .map(this::fromGeneratedQuestion)
                .toList();
        List<PlannedQuestion> plannedQuestions = interleave(fixedQuestions, generatedQuestions);
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
                questionCount,
                maxQuestionCount,
                sourceQuotas,
                allocations,
                normalizedPreparationPromptVersion,
                generated.promptVersion(),
                modelName);
        return new InterviewQuestionGenerationResult(
                generated.targetRole(),
                generated.promptVersion(),
                plannedQuestions.stream().map(PlannedQuestion::question).toList(),
                configuration);
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
        return new PlannedQuestion(question, primarySource, List.copyOf(sources));
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

    private record PlannedQuestion(
            InterviewQuestion question,
            InterviewQuestionSource primarySource,
            List<InterviewQuestionSource> supportingSources
    ) {
    }
}
