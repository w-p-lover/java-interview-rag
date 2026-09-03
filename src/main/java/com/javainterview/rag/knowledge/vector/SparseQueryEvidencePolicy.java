package com.javainterview.rag.knowledge.vector;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 使用技术锚点覆盖率判断 BM25 候选是否具备最小证据。 */
final class SparseQueryEvidencePolicy {

    static final String CODE = "technical_anchor_coverage_v1";
    private static final Pattern TECHNICAL_ANCHOR_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_$])[A-Za-z][A-Za-z0-9_$]*(?:[-.][A-Za-z0-9_$]+)*(?![A-Za-z0-9_$])");

    boolean hasEnoughEvidence(String query, List<MarkdownSearchResult> results) {
        if (results.isEmpty()) {
            return false;
        }
        Set<String> queryAnchors = technicalAnchors(query);
        if (queryAnchors.isEmpty()) {
            return true;
        }
        Set<String> candidateAnchors = new LinkedHashSet<>();
        for (MarkdownSearchResult result : results) {
            candidateAnchors.addAll(technicalAnchors(String.join(" ", result.headingPath())));
            candidateAnchors.addAll(technicalAnchors(result.content()));
        }
        long matchedAnchorCount = queryAnchors.stream().filter(candidateAnchors::contains).count();
        int requiredAnchorCount = (queryAnchors.size() + 1) / 2;
        return matchedAnchorCount >= requiredAnchorCount;
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
