package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluations/answers")
@Tag(name = "RAG 回答评估", description = "运行固定 RAG 回答评估集")
public class RagAnswerEvaluationController {

    private final RagAnswerEvaluationService evaluationService;

    public RagAnswerEvaluationController(RagAnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @Operation(summary = "运行 RAG 回答评估")
    public RagAnswerEvaluationReport evaluate() {
        return evaluationService.evaluate();
    }
}
