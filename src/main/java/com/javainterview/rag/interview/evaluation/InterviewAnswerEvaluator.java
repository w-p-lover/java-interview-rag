package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;

import java.util.List;

public interface InterviewAnswerEvaluator {

    InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile,
                                       InterviewQuestion question,
                                       String answer);

    default InterviewAnswerAssessment evaluateForReview(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            List<InterviewAnswerPreviousAttempt> previousAttempts) {
        return evaluate(profile, question, answer);
    }

    default InterviewAnswerAssessment evaluateBaseline(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            String baselineRunId,
            String baselineCaseId) {
        return evaluate(profile, question, answer);
    }

    default String promptVersion() {
        return "unknown";
    }

    default String rubricVersion() {
        return "unknown";
    }

    default String modelName() {
        return "unknown";
    }
}
