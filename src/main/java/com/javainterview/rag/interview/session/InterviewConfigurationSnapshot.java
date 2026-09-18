package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestionAllocation;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public record InterviewConfigurationSnapshot(
        String strategyVersion,
        InterviewSessionMode mode,
        InterviewSkill skill,
        InterviewDifficulty difficulty,
        int initialQuestionCount,
        int maxQuestionCount,
        Map<InterviewQuestionSource, Integer> sourceQuotas,
        List<InterviewQuestionAllocation> questionAllocations,
        String preparationPromptVersion,
        String questionPromptVersion,
        String modelName
) {

    public InterviewConfigurationSnapshot {
        strategyVersion = requireText(strategyVersion, "组卷策略版本不能为空");
        mode = Objects.requireNonNull(mode, "面试模式不能为空");
        skill = Objects.requireNonNull(skill, "面试 Skill 不能为空");
        difficulty = Objects.requireNonNull(difficulty, "面试难度不能为空");
        if (initialQuestionCount < 3 || initialQuestionCount > 8) {
            throw new IllegalArgumentException("首轮题数必须在 3 到 8 之间");
        }
        if (maxQuestionCount < initialQuestionCount || maxQuestionCount > 12) {
            throw new IllegalArgumentException("整场题数上限必须在首轮题数到 12 之间");
        }
        sourceQuotas = Map.copyOf(Objects.requireNonNull(sourceQuotas, "题目来源配额不能为空"));
        questionAllocations = List.copyOf(Objects.requireNonNull(
                questionAllocations, "题目来源分配不能为空"));
        if (questionAllocations.size() != initialQuestionCount) {
            throw new IllegalArgumentException("题目来源分配数量必须与首轮题数一致");
        }
        for (int index = 0; index < questionAllocations.size(); index++) {
            if (questionAllocations.get(index).questionIndex() != index) {
                throw new IllegalArgumentException("题目来源分配必须按题号连续排列");
            }
        }
        Map<InterviewQuestionSource, Integer> actualQuotas = new EnumMap<>(InterviewQuestionSource.class);
        questionAllocations.forEach(allocation -> actualQuotas.merge(
                allocation.primarySource(), 1, Integer::sum));
        if (!actualQuotas.equals(sourceQuotas)) {
            throw new IllegalArgumentException("题目来源配额与实际分配不一致");
        }
        preparationPromptVersion = requireText(
                preparationPromptVersion, "准备画像 Prompt 版本不能为空");
        questionPromptVersion = requireText(questionPromptVersion, "出题 Prompt 版本不能为空");
        modelName = requireText(modelName, "模型名称不能为空");
    }

    public static InterviewConfigurationSnapshot legacy(InterviewSessionMode mode,
                                                         int initialQuestionCount,
                                                         int maxQuestionCount) {
        List<InterviewQuestionAllocation> allocations = IntStream.range(0, initialQuestionCount)
                .mapToObj(index -> new InterviewQuestionAllocation(
                        index, InterviewQuestionSource.LEGACY, List.of()))
                .toList();
        return new InterviewConfigurationSnapshot(
                "legacy-v1",
                mode,
                InterviewSkill.GENERAL,
                InterviewDifficulty.INTERMEDIATE,
                initialQuestionCount,
                maxQuestionCount,
                Map.of(InterviewQuestionSource.LEGACY, initialQuestionCount),
                allocations,
                "unknown",
                "unknown",
                "unknown");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
