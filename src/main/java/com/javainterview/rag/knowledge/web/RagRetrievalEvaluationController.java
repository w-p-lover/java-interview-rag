package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationComparisonReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.evaluation.HybridRetrievalEvaluationService;
import com.javainterview.rag.knowledge.evaluation.HybridRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagAdjacentContextEvaluationReport;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluations/retrieval")
@Tag(name = "RAG 检索评估", description = "检索质量和查询拆分评估")
public class RagRetrievalEvaluationController {

    private final RagRetrievalEvaluationService evaluationService;
    private final HybridRetrievalEvaluationService hybridEvaluationService;

    public RagRetrievalEvaluationController(RagRetrievalEvaluationService evaluationService) {
        this(evaluationService, null);
    }

    @Autowired
    public RagRetrievalEvaluationController(RagRetrievalEvaluationService evaluationService,
                                             HybridRetrievalEvaluationService hybridEvaluationService) {
        this.evaluationService = evaluationService;
        this.hybridEvaluationService = hybridEvaluationService;
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

    @GetMapping("/hybrid")
    @Operation(summary = "运行 Dense、BM25 和 Hybrid 对照评估")
    public HybridRetrievalEvaluationReport evaluateHybrid() {
        if (hybridEvaluationService == null) {
            throw new IllegalStateException("Hybrid 检索评估服务未配置");
        }
        return hybridEvaluationService.evaluate();
    }

    @GetMapping("/adjacent-context")
    @Operation(summary = "运行相邻 Chunk 上下文开启/关闭对照评估")
    public RagAdjacentContextEvaluationReport compareAdjacentContext() {
        if (hybridEvaluationService == null) {
            throw new IllegalStateException("Hybrid 检索评估服务未配置");
        }
        return hybridEvaluationService.compareAdjacentContext();
    }
}
