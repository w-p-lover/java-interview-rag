package com.javainterview.rag.interview.question;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record InterviewSkillTrainingProfile(
        String version,
        InterviewSkill skill,
        List<Category> categories
) {

    public InterviewSkillTrainingProfile {
        version = requireText(version, "Skill 训练配置版本不能为空");
        skill = Objects.requireNonNull(skill, "Skill 训练配置不能为空");
        categories = List.copyOf(Objects.requireNonNull(categories, "Skill 训练分类不能为空"));
        if (categories.size() != 3) {
            throw new IllegalArgumentException("每个 Skill 必须包含三个训练分类");
        }
        if (categories.stream().map(Category::name).distinct().count() != categories.size()) {
            throw new IllegalArgumentException("Skill 训练分类不能重复");
        }
    }

    public TrainingPlan allocate(int questionCount, String focusDimension) {
        if (questionCount < 1 || questionCount > 8) {
            throw new IllegalArgumentException("训练题数必须在 1 到 8 之间");
        }
        String normalizedFocus = focusDimension == null || focusDimension.isBlank()
                ? null : focusDimension.strip();
        int focusIndex = -1;
        if (normalizedFocus != null) {
            for (int index = 0; index < categories.size(); index++) {
                if (categories.get(index).name().equals(normalizedFocus)) {
                    focusIndex = index;
                    break;
                }
            }
            if (focusIndex < 0) {
                throw new IllegalArgumentException("训练重点不属于当前 Skill");
            }
        }

        int[] counts = new int[categories.size()];
        if (focusIndex < 0) {
            for (int index = 0; index < questionCount; index++) {
                counts[index % categories.size()]++;
            }
        } else {
            int focusedCount = Math.min(2, questionCount);
            counts[focusIndex] = focusedCount;
            int remaining = questionCount - focusedCount;
            List<Integer> cycle = new ArrayList<>(categories.size());
            for (int index = 0; index < categories.size(); index++) {
                if (index != focusIndex) {
                    cycle.add(index);
                }
            }
            cycle.add(focusIndex);
            for (int index = 0; index < remaining; index++) {
                counts[cycle.get(index % cycle.size())]++;
            }
        }

        List<CategoryQuota> quotas = new ArrayList<>(categories.size());
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            quotas.add(new CategoryQuota(
                    category.name(), category.objective(), counts[index], index == focusIndex));
        }
        return new TrainingPlan(version, skill, questionCount, normalizedFocus, quotas);
    }

    public record Category(String name, String objective) {
        public Category {
            name = requireText(name, "Skill 训练分类名称不能为空");
            objective = requireText(objective, "Skill 训练分类目标不能为空");
        }
    }

    public record CategoryQuota(
            String category,
            String objective,
            int questionCount,
            boolean focused
    ) {
        public CategoryQuota {
            category = requireText(category, "Skill 训练分类名称不能为空");
            objective = requireText(objective, "Skill 训练分类目标不能为空");
            if (questionCount < 0) {
                throw new IllegalArgumentException("Skill 分类题量不能小于 0");
            }
        }
    }

    public record TrainingPlan(
            String version,
            InterviewSkill skill,
            int questionCount,
            String focusDimension,
            List<CategoryQuota> categories
    ) {
        public TrainingPlan {
            version = requireText(version, "Skill 训练配置版本不能为空");
            skill = Objects.requireNonNull(skill, "Skill 不能为空");
            categories = List.copyOf(Objects.requireNonNull(categories, "分类配额不能为空"));
            int actualCount = categories.stream().mapToInt(CategoryQuota::questionCount).sum();
            if (actualCount != questionCount) {
                throw new IllegalArgumentException("分类配额与训练题数不一致");
            }
        }

        public Map<String, Integer> quotaMap() {
            Map<String, Integer> result = new LinkedHashMap<>();
            categories.stream().filter(item -> item.questionCount() > 0)
                    .forEach(item -> result.put(item.category(), item.questionCount()));
            return result;
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
