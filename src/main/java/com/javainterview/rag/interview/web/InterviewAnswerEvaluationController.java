package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.FixedInterviewAnswerEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/answers")
@Tag(name = "回答评价", description = "固定面试回答评价基线")
public class InterviewAnswerEvaluationController {

    private final FixedInterviewAnswerEvaluationService evaluationService;

    public InterviewAnswerEvaluationController(FixedInterviewAnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @Operation(summary = "运行回答评价基线")
    public FixedInterviewAnswerEvaluationService.Report evaluate() {
        return evaluationService.evaluate();
    }
}
