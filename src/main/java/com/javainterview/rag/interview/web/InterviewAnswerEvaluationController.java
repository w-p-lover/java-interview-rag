package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.FixedInterviewAnswerEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/answers")
public class InterviewAnswerEvaluationController {

    private final FixedInterviewAnswerEvaluationService evaluationService;

    public InterviewAnswerEvaluationController(FixedInterviewAnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public FixedInterviewAnswerEvaluationService.Report evaluate() {
        return evaluationService.evaluate();
    }
}
