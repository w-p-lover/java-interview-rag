package com.javainterview.rag.knowledge.vector;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 使用技术锚点和已确认的中文近域 Hard Negative 短语判断 BM25 候选是否具备最小证据。 */
final class SparseQueryEvidencePolicy {

    static final String CODE = "technical_anchor_coverage_v2";
    private static final Pattern TECHNICAL_ANCHOR_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_$])[A-Za-z][A-Za-z0-9_$]*(?:[-.][A-Za-z0-9_$]+)*(?![A-Za-z0-9_$])");
    private static final List<String> CURATED_UNSUPPORTED_HAN_PHRASES = List.of(
            "定时任务", "全文索引", "停顿时间");

    boolean hasEnoughEvidence(String query, List<MarkdownSearchResult> results) {
        if (results.isEmpty()) {
            return false;
        }
        Set<String> queryAnchors = technicalAnchors(query);
        String candidateText = candidateText(results);
        if (!queryAnchors.isEmpty()) {
            Set<String> candidateAnchors = technicalAnchors(candidateText);
            long matchedAnchorCount = queryAnchors.stream().filter(candidateAnchors::contains).count();
            int requiredAnchorCount = (queryAnchors.size() + 1) / 2;
            return matchedAnchorCount >= requiredAnchorCount;
        }
        if (CURATED_UNSUPPORTED_HAN_PHRASES.stream()
                .anyMatch(phrase -> query.contains(phrase) && !candidateText.contains(phrase))) {
            return false;
        }
        return true;
    }

    private String candidateText(List<MarkdownSearchResult> results) {
        StringBuilder text = new StringBuilder();
        for (MarkdownSearchResult result : results) {
            text.append(resultText(result)).append(' ');
        }
        return text.toString();
    }

    private String resultText(MarkdownSearchResult result) {
        return String.join(" ", result.headingPath()) + " " + result.content();
    }

    private Set<String> technicalAnchors(String text) {
        Set<String> anchors = new LinkedHashSet<>();
        Matcher matcher = TECHNICAL_ANCHOR_PATTERN.matcher(text);
        while (matcher.find()) {
            String anchor = matcher.group().toLowerCase(Locale.ROOT);
            if (anchor.length() >= 2) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

}
