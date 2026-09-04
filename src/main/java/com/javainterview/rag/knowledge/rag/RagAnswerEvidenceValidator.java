package com.javainterview.rag.knowledge.rag;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RagAnswerEvidenceValidator {

    private static final double MIN_ANCHOR_COVERAGE = 0.5;
    private static final Pattern SOURCE_SENTENCE = Pattern.compile("[^。！？!?；;\\r\\n]+");
    private static final Pattern LATIN_OR_NUMBER = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_+#.-]*|\\d+(?:\\.\\d+)?");
    private static final Pattern HAN = Pattern.compile("[\\p{IsHan}]+");
    private static final Pattern NEGATED_PHRASE = Pattern.compile(
            "(?:不允许|不支持|不能|无法|禁止|没有|不会|并非|不是|不)([^，,。！？!?；;\\r\\n]{1,24})");
    private static final Set<String> STOP_WORDS = Set.of(
            "使用", "可以", "通常", "属于", "如果", "因为", "所以", "以及", "然后", "需要", "通过", "进行");

    private final RagAnswerCitationValidator citationValidator = new RagAnswerCitationValidator();

    public RagAnswerEvidenceValidation validate(String answer, List<MarkdownSearchResult> sources) {
        RagAnswerCitationValidation citationValidation = citationValidator.validate(answer, sources.size());
        List<RagAnswerClaimEvidence> claims = citationValidation.claims().stream()
                .map(claim -> evaluateClaim(claim, sources))
                .toList();
        boolean valid = citationValidation.valid()
                && claims.stream().allMatch(claim -> claim.status() == RagAnswerClaimEvidenceStatus.SUPPORTED);
        return new RagAnswerEvidenceValidation(valid, claims);
    }

    private RagAnswerClaimEvidence evaluateClaim(RagAnswerClaimCitation claim,
                                                 List<MarkdownSearchResult> sources) {
        if (claim.status() != RagAnswerClaimCitationStatus.VALID_CITATION) {
            return new RagAnswerClaimEvidence(claim.claim(), claim.sourceNumbers(),
                    RagAnswerClaimEvidenceStatus.CITATION_INVALID, 0.0);
        }

        String claimText = removeCitations(claim.claim());
        Set<String> claimAnchors = anchors(claimText);
        Candidate best = null;
        for (Integer sourceNumber : claim.sourceNumbers()) {
            String content = sources.get(sourceNumber - 1).content();
            List<String> sentences = sourceSentences(content);
            for (int index = 0; index < sentences.size(); index++) {
                best = betterCandidate(best, candidate(claimAnchors, claimText, sentences.get(index)));
                if (index + 1 < sentences.size()) {
                    best = betterCandidate(best, candidate(claimAnchors, claimText,
                            sentences.get(index) + "。" + sentences.get(index + 1)));
                }
            }
        }
        if (best == null || best.coverage() < MIN_ANCHOR_COVERAGE) {
            return new RagAnswerClaimEvidence(claim.claim(), claim.sourceNumbers(),
                    RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT,
                    best == null ? 0.0 : best.coverage());
        }
        RagAnswerClaimEvidenceStatus status = best.conflictingPolarity()
                ? RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING
                : RagAnswerClaimEvidenceStatus.SUPPORTED;
        return new RagAnswerClaimEvidence(claim.claim(), claim.sourceNumbers(), status, best.coverage());
    }

    private Candidate candidate(Set<String> claimAnchors, String claimText, String sourceText) {
        Set<String> sourceAnchors = anchors(sourceText);
        Set<String> sharedAnchors = new HashSet<>(claimAnchors);
        sharedAnchors.retainAll(sourceAnchors);
        double coverage = claimAnchors.isEmpty() ? 0.0 : (double) sharedAnchors.size() / claimAnchors.size();
        return new Candidate(coverage, conflictingPolarity(claimText, sourceText, sharedAnchors));
    }

    private Candidate betterCandidate(Candidate current, Candidate candidate) {
        return current == null || candidate.coverage() > current.coverage() ? candidate : current;
    }

    private List<String> sourceSentences(String content) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SOURCE_SENTENCE.matcher(content);
        while (matcher.find()) {
            String sentence = matcher.group().strip();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    private boolean conflictingPolarity(String claimText, String sourceText, Set<String> sharedAnchors) {
        Set<String> claimNegatedAnchors = negatedAnchors(claimText, sharedAnchors);
        Set<String> sourceNegatedAnchors = negatedAnchors(sourceText, sharedAnchors);
        if (claimNegatedAnchors.isEmpty() || sourceNegatedAnchors.isEmpty()) {
            return claimNegatedAnchors.isEmpty() != sourceNegatedAnchors.isEmpty();
        }
        return claimNegatedAnchors.stream().noneMatch(sourceNegatedAnchors::contains);
    }

    private Set<String> negatedAnchors(String text, Set<String> sharedAnchors) {
        Set<String> negatedAnchors = new HashSet<>();
        Matcher matcher = NEGATED_PHRASE.matcher(text);
        while (matcher.find()) {
            Set<String> phraseAnchors = anchors(matcher.group(1));
            phraseAnchors.retainAll(sharedAnchors);
            negatedAnchors.addAll(phraseAnchors);
        }
        return negatedAnchors;
    }

    private Set<String> anchors(String text) {
        Set<String> anchors = new LinkedHashSet<>();
        Matcher latinMatcher = LATIN_OR_NUMBER.matcher(text.toLowerCase(Locale.ROOT));
        while (latinMatcher.find()) {
            anchors.add(latinMatcher.group());
        }
        Matcher hanMatcher = HAN.matcher(text);
        while (hanMatcher.find()) {
            String value = hanMatcher.group();
            if (value.length() == 1) {
                anchors.add(value);
                continue;
            }
            for (int index = 0; index < value.length() - 1; index++) {
                String bigram = value.substring(index, index + 2);
                if (!STOP_WORDS.contains(bigram)) {
                    anchors.add(bigram);
                }
            }
        }
        return anchors;
    }

    private String removeCitations(String claim) {
        return claim.replaceAll("\\[资料\\s*\\d+]", "").strip();
    }

    private record Candidate(double coverage, boolean conflictingPolarity) {
    }
}
