package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.util.List;

public interface InterviewAnswerEvaluator {

    InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile,
                                       InterviewQuestion question,
                                       String answer);

    default InterviewAnswerAssessment evaluate(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            InterviewSkill skill) {
        return evaluate(profile, question, answer);
    }

    /** 使用当前会话的受控上下文评价回答。 */
    default InterviewAnswerAssessment evaluateWithContext(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            String conversationContext) {
        return evaluate(profile, question, answer);
    }

    default InterviewAnswerAssessment evaluateWithContext(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            String conversationContext,
            InterviewSkill skill) {
        return evaluateWithContext(profile, question, answer, conversationContext);
    }

    default InterviewAnswerAssessment evaluateForReview(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            List<InterviewAnswerPreviousAttempt> previousAttempts) {
        return evaluate(profile, question, answer);
    }

    default InterviewAnswerAssessment evaluateForReview(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            List<InterviewAnswerPreviousAttempt> previousAttempts,
            InterviewSkill skill) {
        return evaluateForReview(profile, question, answer, previousAttempts);
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
