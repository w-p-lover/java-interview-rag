package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Objects;

public record ResumeEvidenceAuditDraft(
        List<FactDraft> facts,
        List<String> missingInformation,
        List<String> risks,
        List<String> suggestions
) {

    private static final int MAX_FACT_COUNT = 30;
    private static final int MAX_LIST_COUNT = 20;

    public ResumeEvidenceAuditDraft {
        facts = List.copyOf(Objects.requireNonNull(facts, "简历事实不能为空"));
        if (facts.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("简历事实不能包含空值");
        }
        if (facts.size() > MAX_FACT_COUNT) {
            throw new IllegalArgumentException("简历事实最多返回 30 条");
        }
        missingInformation = copyTexts(missingInformation, "信息缺失不能为空");
        risks = copyTexts(risks, "风险判断不能为空");
        suggestions = copyTexts(suggestions, "表达建议不能为空");
    }

    private static List<String> copyTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.size() > MAX_LIST_COUNT) {
            throw new IllegalArgumentException("审计列表最多返回 20 条");
        }
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return values.stream().map(String::strip).distinct().toList();
    }

    public record FactDraft(
            ResumeEvidenceAuditResult.FactType type,
            String statement,
            String evidence
    ) {

        public FactDraft {
            Objects.requireNonNull(type, "事实类型不能为空");
            statement = requireText(statement, 1000, "事实描述不能为空", "事实描述不能超过 1000 个字符");
            evidence = requireText(evidence, 500, "事实证据不能为空", "事实证据不能超过 500 个字符");
        }

        private static String requireText(String value, int maxLength,
                                          String requiredMessage, String lengthMessage) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(requiredMessage);
            }
            String normalized = value.strip();
            if (normalized.length() > maxLength) {
                throw new IllegalArgumentException(lengthMessage);
            }
            return normalized;
        }
    }
}
