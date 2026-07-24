package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/evaluation-cases")
public class RagEvaluationDatasetController {

    private final FixedRagEvaluationDataset dataset;

    public RagEvaluationDatasetController(FixedRagEvaluationDataset dataset) {
        this.dataset = dataset;
    }

    @GetMapping
    public List<RagEvaluationCase> findAll() {
        return dataset.findAll();
    }
}
