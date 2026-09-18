package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record BankQuestion(
        String id, String generationTaskId, String source, String documentVersion,
        InterviewSkill skill, InterviewDifficulty difficulty, String category, String question,
        String topicSummary, String referenceAnswer, List<String> keyPoints, String scoringRubric,
        List<FollowUp> followUps, List<MarkdownSearchResult> evidence, Status status, long version,
        int useCount, LocalDateTime lastUsedAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public enum Status { DRAFT, ACTIVE, ARCHIVED, STALE }

    public BankQuestion {
        id = text(id, 36, "题目 ID");
        source = text(source, 500, "资料来源");
        documentVersion = text(documentVersion, 100, "资料版本");
        Objects.requireNonNull(skill, "请选择 Skill");
        Objects.requireNonNull(difficulty, "请选择难度");
        category = text(category, 100, "分类");
        question = text(question, 2000, "题目");
        topicSummary = text(topicSummary, 1000, "考察目标");
        referenceAnswer = text(referenceAnswer, 8000, "参考答案");
        scoringRubric = text(scoringRubric, 4000, "评分标准");
        keyPoints = texts(keyPoints, 20, "答案要点");
        followUps = List.copyOf(Objects.requireNonNull(followUps, "固定追问不能为空"));
        if (followUps.size() > 2) {
            throw new IllegalArgumentException("固定追问最多 2 个");
        }
        evidence = List.copyOf(Objects.requireNonNull(evidence, "题目证据不能为空"));
        if (evidence.isEmpty() || evidence.size() > 3) {
            throw new IllegalArgumentException("题目证据必须来自绑定资料的同一版本，且为 1 到 3 条");
        }
        for (MarkdownSearchResult item : evidence) {
            if (!sourceEquals(item, source, documentVersion)) {
                throw new IllegalArgumentException("题目证据必须来自绑定资料的同一版本");
            }
        }
        Objects.requireNonNull(status, "题目状态不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");
    }

    private static boolean sourceEquals(MarkdownSearchResult item, String source, String version) {
        return item.source().equals(source) && item.documentVersion().equals(version);
    }

    public BankQuestion withState(Status nextStatus, long nextVersion, LocalDateTime now) {
        return new BankQuestion(id, generationTaskId, source, documentVersion, skill, difficulty,
                category, question, topicSummary, referenceAnswer, keyPoints, scoringRubric,
                followUps, evidence, nextStatus, nextVersion, useCount, lastUsedAt, createdAt, now);
    }

    public record FollowUp(String question, String referenceAnswer,
                           List<String> keyPoints, String scoringRubric) {
        public FollowUp {
            question = text(question, 2000, "追问题目");
            referenceAnswer = text(referenceAnswer, 8000, "追问参考答案");
            keyPoints = texts(keyPoints, 20, "追问要点");
            scoringRubric = text(scoringRubric, 4000, "追问评分标准");
        }
    }

    public static String text(String value, int limit, String name) {
        if (value == null || value.isBlank() || value.strip().length() > limit) {
            throw new IllegalArgumentException(name + "不能为空且不能超过 " + limit + " 个字符");
        }
        return value.strip();
    }

    private static List<String> texts(List<String> values, int limit, String name) {
        if (values == null || values.isEmpty() || values.size() > limit) {
            throw new IllegalArgumentException(name + "必须为 1 到 " + limit + " 项");
        }
        return values.stream().map(value -> text(value, 1000, name)).toList();
    }
}
