package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/evaluation-cases")
@Tag(name = "RAG 评估集", description = "查看固定评估问题集")
public class RagEvaluationDatasetController {

    private final FixedRagEvaluationDataset dataset;

    public RagEvaluationDatasetController(FixedRagEvaluationDataset dataset) {
        this.dataset = dataset;
    }

    @GetMapping
    @Operation(summary = "查询 RAG 评估问题")
    public List<RagEvaluationCase> findAll() {
        return dataset.findAll();
    }
}
