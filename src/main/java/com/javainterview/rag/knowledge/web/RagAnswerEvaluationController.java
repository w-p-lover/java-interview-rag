package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluations/answers")
public class RagAnswerEvaluationController {

    private final RagAnswerEvaluationService evaluationService;

    public RagAnswerEvaluationController(RagAnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public RagAnswerEvaluationReport evaluate() {
        return evaluationService.evaluate();
    }
}
