package com.javainterview.rag.knowledge.evaluation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RagEvaluationQueryPlanner {

    private static final int MAX_QUERY_COUNT = 4;
    private static final int MIN_QUERY_LENGTH = 4;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[？?。；;]+");
    private static final Pattern CLAUSE_BOUNDARY = Pattern.compile("[，,]+");
    private static final Pattern COORDINATE_BOUNDARY = Pattern.compile("与|和|以及");

    public List<String> plan(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("评估问题不能为空");
        }

        String originalQuestion = question.strip();
        String normalizedQuestion = normalize(originalQuestion);
        Set<String> candidates = new LinkedHashSet<>();

        List<String> sentences = split(normalizedQuestion, SENTENCE_BOUNDARY);
        addAll(candidates, sentences);

        if (containsCoordinateCue(normalizedQuestion)) {
            for (String sentence : sentences) {
                addAll(candidates, splitFirst(sentence, COORDINATE_BOUNDARY));
            }
        }
        for (String sentence : sentences) {
            addAll(candidates, split(sentence, CLAUSE_BOUNDARY));
        }

        List<String> queries = new ArrayList<>();
        queries.add(originalQuestion);
        candidates.stream()
                .filter(candidate -> !candidate.equals(normalizedQuestion))
                .limit(MAX_QUERY_COUNT - 1L)
                .forEach(queries::add);
        return List.copyOf(queries);
    }

    private boolean containsCoordinateCue(String question) {
        return question.contains("分别") || question.contains("各自") || question.contains("两者");
    }

    private List<String> split(String value, Pattern boundary) {
        return boundary.splitAsStream(value)
                .map(this::normalize)
                .filter(part -> part.length() >= MIN_QUERY_LENGTH)
                .toList();
    }

    private List<String> splitFirst(String value, Pattern boundary) {
        return java.util.Arrays.stream(boundary.split(value, 2))
                .map(this::normalize)
                .filter(part -> part.length() >= MIN_QUERY_LENGTH)
                .toList();
    }

    private void addAll(Set<String> queries, List<String> candidates) {
        for (String candidate : candidates) {
            if (queries.size() >= MAX_QUERY_COUNT) {
                return;
            }
            add(queries, candidate);
        }
    }

    private void add(Set<String> queries, String candidate) {
        if (candidate.length() >= MIN_QUERY_LENGTH) {
            queries.add(candidate);
        }
    }

    private String normalize(String value) {
        return value.strip().replaceAll("[？?。；;]+$", "").strip();
    }
}
