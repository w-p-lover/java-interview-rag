package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/evaluation-cases/structure")
@Tag(name = "文档结构评估集", description = "查看文档页码和块类型评估问题")
public class RagStructureEvaluationDatasetController {

    private final FixedRagStructureEvaluationDataset dataset;

    public RagStructureEvaluationDatasetController(FixedRagStructureEvaluationDataset dataset) {
        this.dataset = dataset;
    }

    @GetMapping
    @Operation(summary = "查询文档结构评估问题")
    public List<RagEvaluationCase> findAll() {
        return dataset.findAll();
    }
}
