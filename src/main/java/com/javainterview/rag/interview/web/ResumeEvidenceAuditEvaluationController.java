package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/resume-audits")
@Tag(name = "简历审计评估", description = "运行固定简历审计评估集")
public class ResumeEvidenceAuditEvaluationController {

    private final FixedResumeEvidenceAuditEvaluationService evaluationService;

    public ResumeEvidenceAuditEvaluationController(
            FixedResumeEvidenceAuditEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @Operation(summary = "运行简历审计评估")
    public FixedResumeEvidenceAuditEvaluationService.Report evaluate() {
        return evaluationService.evaluate();
    }
}
