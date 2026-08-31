package com.javainterview.rag.interview.matching;

import java.util.List;
import java.util.Objects;

public record ResumeJobMatchResult(
        String requestId,
        String role,
        String promptVersion,
        String summary,
        List<Assessment> assessments
) {

    public ResumeJobMatchResult {
        requestId = requireText(requestId, "岗位匹配请求标识不能为空");
        role = requireText(role, "岗位匹配角色不能为空");
        promptVersion = requireText(promptVersion, "岗位匹配 Prompt 版本不能为空");
        summary = requireText(summary, "岗位匹配总结不能为空");
        assessments = List.copyOf(Objects.requireNonNull(assessments, "岗位匹配明细不能为空"));
        if (assessments.isEmpty()) {
            throw new IllegalArgumentException("岗位匹配明细不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    public record Assessment(
            String requirement,
            String requirementEvidence,
            MatchStatus status,
            String resumeEvidence,
            String explanation
    ) {

        public Assessment {
            requirement = requireText(requirement, "岗位要求不能为空");
            requirementEvidence = requireText(requirementEvidence, "岗位要求证据不能为空");
            Objects.requireNonNull(status, "岗位匹配状态不能为空");
            if (status == MatchStatus.MISSING) {
                if (resumeEvidence != null && !resumeEvidence.isBlank()) {
                    throw new IllegalArgumentException("缺口项不能携带简历证据");
                }
                resumeEvidence = null;
            } else {
                resumeEvidence = requireText(resumeEvidence, "匹配项简历证据不能为空");
            }
            explanation = requireText(explanation, "岗位匹配说明不能为空");
        }
    }

    public enum MatchStatus {
        MATCHED,
        PARTIAL,
        MISSING,
        NEEDS_VERIFICATION
    }
}
