package com.javainterview.rag.interview.matching;

import java.util.List;

record ResumeJobMatchDraft(
        String summary,
        List<AssessmentDraft> assessments
) {

    record AssessmentDraft(
            String requirement,
            String requirementEvidence,
            MatchStatus status,
            String resumeEvidence,
            String explanation
    ) {
    }

    enum MatchStatus {
        MATCHED,
        PARTIAL,
        MISSING,
        NEEDS_VERIFICATION
    }
}
