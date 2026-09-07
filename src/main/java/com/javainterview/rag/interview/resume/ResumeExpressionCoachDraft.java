package com.javainterview.rag.interview.resume;

import java.util.List;

record ResumeExpressionCoachDraft(List<CandidateDraft> candidates) {

    record CandidateDraft(
            String text,
            String rationale,
            List<Integer> supportingFactNumbers,
            List<String> reviewPoints
    ) {
    }
}
