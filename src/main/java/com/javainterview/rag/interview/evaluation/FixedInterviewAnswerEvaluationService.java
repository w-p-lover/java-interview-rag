package com.javainterview.rag.interview.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FixedInterviewAnswerEvaluationService {

    private final FixedInterviewAnswerEvaluationDataset dataset;
    private final InterviewAnswerEvaluator evaluator;

    public FixedInterviewAnswerEvaluationService(FixedInterviewAnswerEvaluationDataset dataset,
                                                  InterviewAnswerEvaluator evaluator) {
        this.dataset = Objects.requireNonNull(dataset, "固定面试回答评分样本不能为空");
        this.evaluator = Objects.requireNonNull(evaluator, "回答评价器不能为空");
    }

    public Report evaluate() {
        List<CaseResult> results = new ArrayList<>();
        for (FixedInterviewAnswerEvaluationDataset.Case evaluationCase : dataset.findAll()) {
            results.add(evaluateCase(evaluationCase));
        }
        int passedCaseCount = (int) results.stream().filter(CaseResult::passed).count();
        return new Report(
                evaluator.promptVersion(),
                evaluator.rubricVersion(),
                evaluator.modelName(),
                results.size(),
                passedCaseCount,
                results);
    }

    private CaseResult evaluateCase(FixedInterviewAnswerEvaluationDataset.Case evaluationCase) {
        try {
            InterviewAnswerEvaluation evaluation = evaluator.evaluateBaseline(
                    evaluationCase.profile(), evaluationCase.question(), evaluationCase.answer()).evaluation();
            boolean passed = !evaluationCase.expectedRejected()
                    && evaluation.overallScore() >= evaluationCase.expectedMinOverallScore()
                    && evaluation.overallScore() <= evaluationCase.expectedMaxOverallScore()
                    && evaluation.knowledgeGrounded() == evaluationCase.expectedKnowledgeGrounded();
            return new CaseResult(
                    evaluationCase.id(),
                    "SUCCEEDED",
                    passed,
                    evaluationCase.expectedMinOverallScore(),
                    evaluationCase.expectedMaxOverallScore(),
                    evaluation.overallScore(),
                    evaluationCase.expectedKnowledgeGrounded(),
                    evaluation.knowledgeGrounded(),
                    null);
        } catch (IllegalArgumentException exception) {
            if (!evaluationCase.expectedRejected()) {
                throw exception;
            }
            return new CaseResult(
                    evaluationCase.id(),
                    "EXPECTED_REJECTION",
                    true,
                    evaluationCase.expectedMinOverallScore(),
                    evaluationCase.expectedMaxOverallScore(),
                    null,
                    evaluationCase.expectedKnowledgeGrounded(),
                    null,
                    exception.getClass().getSimpleName());
        }
    }

    public record Report(
            String promptVersion,
            String rubricVersion,
            String modelName,
            int totalCaseCount,
            int passedCaseCount,
            List<CaseResult> cases
    ) {

        public Report {
            cases = List.copyOf(Objects.requireNonNull(cases, "评分样本结果不能为空"));
        }
    }

    public record CaseResult(
            String caseId,
            String status,
            boolean passed,
            int expectedMinOverallScore,
            int expectedMaxOverallScore,
            Integer actualOverallScore,
            boolean expectedKnowledgeGrounded,
            Boolean actualKnowledgeGrounded,
            String errorType
    ) {
    }
}
