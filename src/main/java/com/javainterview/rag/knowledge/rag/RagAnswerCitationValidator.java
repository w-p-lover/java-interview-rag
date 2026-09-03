package com.javainterview.rag.knowledge.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RagAnswerCitationValidator {

    private static final Pattern CITATION = Pattern.compile("\\[资料\\s*(\\d+)]");
    private static final Pattern CLAIM_SEPARATOR = Pattern.compile("(?<=[。！？!?；;])|\\R+");

    RagAnswerCitationValidation validate(String answer, int sourceCount) {
        List<RagAnswerClaimCitation> claims = new ArrayList<>();
        Set<Integer> citedSourceNumbers = new LinkedHashSet<>();
        Set<Integer> invalidSourceNumbers = new LinkedHashSet<>();

        for (String segment : CLAIM_SEPARATOR.split(answer)) {
            String claim = segment.strip();
            if (!hasClaimText(claim)) {
                continue;
            }
            List<Integer> claimSourceNumbers = sourceNumbers(claim);
            citedSourceNumbers.addAll(claimSourceNumbers);
            List<Integer> invalidClaimSources = claimSourceNumbers.stream()
                    .filter(sourceNumber -> sourceNumber < 1 || sourceNumber > sourceCount)
                    .toList();
            invalidSourceNumbers.addAll(invalidClaimSources);
            RagAnswerClaimCitationStatus status = claimSourceNumbers.isEmpty()
                    ? RagAnswerClaimCitationStatus.MISSING_CITATION
                    : invalidClaimSources.isEmpty()
                            ? RagAnswerClaimCitationStatus.VALID_CITATION
                            : RagAnswerClaimCitationStatus.INVALID_CITATION;
            claims.add(new RagAnswerClaimCitation(claim, claimSourceNumbers, status));
        }

        boolean valid = !claims.isEmpty()
                && invalidSourceNumbers.isEmpty()
                && claims.stream().allMatch(
                        claim -> claim.status() == RagAnswerClaimCitationStatus.VALID_CITATION);
        return new RagAnswerCitationValidation(valid, List.copyOf(citedSourceNumbers),
                List.copyOf(invalidSourceNumbers), claims);
    }

    private List<Integer> sourceNumbers(String claim) {
        List<Integer> result = new ArrayList<>();
        Matcher matcher = CITATION.matcher(claim);
        while (matcher.find()) {
            try {
                result.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException exception) {
                result.add(Integer.MAX_VALUE);
            }
        }
        return List.copyOf(result);
    }

    private boolean hasClaimText(String claim) {
        String withoutCitations = CITATION.matcher(claim).replaceAll("");
        String structuralText = withoutCitations.strip().replaceAll("^[#>*\\-\\s]+", "")
                .replaceAll("[*_`]+", "");
        if (structuralText.isBlank() || structuralText.endsWith(":" ) || structuralText.endsWith("：")) {
            return false;
        }
        if (structuralText.matches("^(?:根据|按照).{0,20}(?:资料|信息).*(?:回答如下|说明如下)[：:]$")) {
            return false;
        }
        return structuralText.codePoints().anyMatch(Character::isLetterOrDigit);
    }
}
