package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Objects;

public record ResumeExpressionCoachResult(
        String requestId,
        String role,
        String promptVersion,
        String targetRole,
        List<Candidate> candidates
) {

    public ResumeExpressionCoachResult {
        requestId = requireText(requestId, "表达教练请求标识不能为空");
        role = requireText(role, "表达教练角色不能为空");
        promptVersion = requireText(promptVersion, "表达教练 Prompt 版本不能为空");
        targetRole = requireText(targetRole, "目标岗位不能为空");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "表达候选不能为空"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("表达候选不能为空");
        }
    }

    public record Candidate(
            String id,
            String text,
            String rationale,
            List<Integer> supportingFactNumbers,
            List<ResumeEvidenceAuditResult.ConfirmedFact> supportingFacts,
            List<String> reviewPoints
    ) {

        public Candidate {
            id = requireText(id, "表达候选标识不能为空");
            text = requireText(text, "表达候选内容不能为空");
            rationale = requireText(rationale, "表达候选说明不能为空");
            supportingFactNumbers = List.copyOf(Objects.requireNonNull(
                    supportingFactNumbers, "表达候选依据编号不能为空"));
            supportingFacts = List.copyOf(Objects.requireNonNull(
                    supportingFacts, "表达候选依据事实不能为空"));
            reviewPoints = List.copyOf(Objects.requireNonNull(reviewPoints, "人工核对项不能为空"));
            if (supportingFactNumbers.isEmpty() || supportingFacts.isEmpty()) {
                throw new IllegalArgumentException("表达候选必须包含依据事实");
            }
            if (supportingFactNumbers.size() != supportingFacts.size()) {
                throw new IllegalArgumentException("表达候选依据编号与事实数量不一致");
            }
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
