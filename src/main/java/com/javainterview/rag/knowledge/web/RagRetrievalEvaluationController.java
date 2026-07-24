package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluations/retrieval")
public class RagRetrievalEvaluationController {

    private final RagRetrievalEvaluationService evaluationService;

    public RagRetrievalEvaluationController(RagRetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public RagRetrievalEvaluationReport evaluate() {
        return evaluationService.evaluate();
    }
}
