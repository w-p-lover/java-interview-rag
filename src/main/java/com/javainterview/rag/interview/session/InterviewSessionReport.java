package com.javainterview.rag.interview.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record InterviewSessionReport(
        int answeredQuestions,
        double averageTechnicalScore,
        double averageCompletenessScore,
        double averageCommunicationScore,
        double overallAverageScore,
        List<String> strengths,
        List<String> gaps,
        LocalDateTime completedAt
) {

    public InterviewSessionReport {
        if (answeredQuestions < 1) {
            throw new IllegalArgumentException("完成报告必须包含回答");
        }
        strengths = List.copyOf(Objects.requireNonNull(strengths, "报告优点不能为空"));
        gaps = List.copyOf(Objects.requireNonNull(gaps, "报告薄弱项不能为空"));
        completedAt = Objects.requireNonNull(completedAt, "面试完成时间不能为空");
    }

    public static InterviewSessionReport from(List<InterviewSessionAnswer> answers,
                                              LocalDateTime completedAt) {
        int count = answers.size();
        double technical = average(answers.stream()
                .mapToInt(answer -> answer.evaluation().technicalScore()).sum(), count);
        double completeness = average(answers.stream()
                .mapToInt(answer -> answer.evaluation().completenessScore()).sum(), count);
        double communication = average(answers.stream()
                .mapToInt(answer -> answer.evaluation().communicationScore()).sum(), count);
        double overall = average(answers.stream()
                .mapToInt(answer -> answer.evaluation().overallScore()).sum(), count);
        List<String> strengths = answers.stream()
                .flatMap(answer -> answer.evaluation().strengths().stream())
                .distinct()
                .limit(5)
                .toList();
        List<String> gaps = answers.stream()
                .flatMap(answer -> answer.evaluation().gaps().stream())
                .distinct()
                .limit(5)
                .toList();
        return new InterviewSessionReport(count, technical, completeness,
                communication, overall, strengths, gaps, completedAt);
    }

    private static double average(int total, int count) {
        return Math.round((total * 10.0) / count) / 10.0;
    }
}
