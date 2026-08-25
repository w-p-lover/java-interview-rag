package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Objects;

public record ResumeEvidenceAuditResult(
        String requestId,
        String role,
        String promptVersion,
        List<ConfirmedFact> confirmedFacts,
        List<String> missingInformation,
        List<String> risks,
        List<String> suggestions
) {

    public ResumeEvidenceAuditResult {
        requestId = requireText(requestId, "审计请求标识不能为空");
        role = requireText(role, "审计角色不能为空");
        promptVersion = requireText(promptVersion, "审计 Prompt 版本不能为空");
        confirmedFacts = List.copyOf(Objects.requireNonNull(confirmedFacts, "已确认事实不能为空"));
        missingInformation = List.copyOf(Objects.requireNonNull(missingInformation, "信息缺失不能为空"));
        risks = List.copyOf(Objects.requireNonNull(risks, "风险判断不能为空"));
        suggestions = List.copyOf(Objects.requireNonNull(suggestions, "表达建议不能为空"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    public record ConfirmedFact(FactType type, String statement, String evidence) {

        public ConfirmedFact {
            Objects.requireNonNull(type, "事实类型不能为空");
            statement = requireText(statement, "事实描述不能为空");
            evidence = requireText(evidence, "事实证据不能为空");
        }
    }

    public enum FactType {
        EDUCATION,
        EMPLOYMENT,
        PROJECT,
        TECHNOLOGY,
        RESPONSIBILITY,
        TIME,
        RESULT
    }
}
