package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/evaluation-cases/structure")
public class RagStructureEvaluationDatasetController {

    private final FixedRagStructureEvaluationDataset dataset;

    public RagStructureEvaluationDatasetController(FixedRagStructureEvaluationDataset dataset) {
        this.dataset = dataset;
    }

    @GetMapping
    public List<RagEvaluationCase> findAll() {
        return dataset.findAll();
    }
}
