package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationComparisonReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluations/retrieval")
@Tag(name = "RAG 检索评估", description = "检索质量和查询拆分评估")
public class RagRetrievalEvaluationController {

    private final RagRetrievalEvaluationService evaluationService;

    public RagRetrievalEvaluationController(RagRetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    @Operation(summary = "运行 RAG 检索评估")
    public RagRetrievalEvaluationReport evaluate() {
        return evaluationService.evaluate();
    }

    @GetMapping("/structure")
    @Operation(summary = "运行文档结构检索评估")
    public RagRetrievalEvaluationReport evaluateStructure() {
        return evaluationService.evaluateStructure();
    }

    @GetMapping("/query-decomposition")
    @Operation(summary = "运行查询拆分对照评估")
    public RagRetrievalEvaluationComparisonReport compareQueryDecomposition() {
        return evaluationService.compareQueryDecomposition();
    }
}
