package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ObservableInterviewAnswerEvaluator implements InterviewAnswerEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ObservableInterviewAnswerEvaluator.class);

    private final InterviewAnswerEvaluator delegate;
    private final InterviewAnswerEvaluationLogStore logStore;

    public ObservableInterviewAnswerEvaluator(InterviewAnswerEvaluator delegate,
                                               InterviewAnswerEvaluationLogStore logStore) {
        this.delegate = Objects.requireNonNull(delegate, "回答评价器不能为空");
        this.logStore = Objects.requireNonNull(logStore, "回答评价日志存储不能为空");
    }

    @Override
    public InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile,
                                              InterviewQuestion question,
                                              String answer) {
        return observe(InterviewAnswerEvaluationLog.Mode.INTERVIEW,
                () -> delegate.evaluate(profile, question, answer));
    }

    @Override
    public InterviewAnswerAssessment evaluateForReview(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            List<InterviewAnswerPreviousAttempt> previousAttempts) {
        return observe(InterviewAnswerEvaluationLog.Mode.REVIEW,
                () -> delegate.evaluateForReview(profile, question, answer, previousAttempts));
    }

    @Override
    public InterviewAnswerAssessment evaluateBaseline(
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer) {
        return observe(InterviewAnswerEvaluationLog.Mode.BASELINE,
                () -> delegate.evaluateBaseline(profile, question, answer));
    }

    @Override
    public String promptVersion() {
        return delegate.promptVersion();
    }

    @Override
    public String rubricVersion() {
        return delegate.rubricVersion();
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    private InterviewAnswerAssessment observe(InterviewAnswerEvaluationLog.Mode mode,
                                              Supplier<InterviewAnswerAssessment> action) {
        long startedAt = System.nanoTime();
        try {
            InterviewAnswerAssessment assessment = action.get();
            record(mode, assessment.evaluation(), startedAt, null);
            return assessment;
        } catch (RuntimeException exception) {
            record(mode, null, startedAt, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void record(InterviewAnswerEvaluationLog.Mode mode,
                        InterviewAnswerEvaluation evaluation,
                        long startedAt,
                        String errorType) {
        try {
            InterviewAnswerEvaluationLog.Status status = evaluation == null
                    ? InterviewAnswerEvaluationLog.Status.FAILED
                    : InterviewAnswerEvaluationLog.Status.SUCCEEDED;
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            InterviewAnswerEvaluationLog evaluationLog = new InterviewAnswerEvaluationLog(
                    null,
                    mode,
                    promptVersion(),
                    rubricVersion(),
                    modelName(),
                    status,
                    evaluation == null ? null : evaluation.knowledgeGrounded(),
                    evaluation == null ? null : evaluation.technicalScore(),
                    evaluation == null ? null : evaluation.completenessScore(),
                    evaluation == null ? null : evaluation.communicationScore(),
                    evaluation == null ? null : evaluation.overallScore(),
                    elapsedMillis,
                    errorType,
                    LocalDateTime.now());
            logStore.record(evaluationLog);
        } catch (RuntimeException exception) {
            log.warn("写入面试回答评价日志失败，不影响本次评价", exception);
        }
    }
}
