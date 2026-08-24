package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/resume-audits")
public class ResumeEvidenceAuditEvaluationController {

    private final FixedResumeEvidenceAuditEvaluationService evaluationService;

    public ResumeEvidenceAuditEvaluationController(
            FixedResumeEvidenceAuditEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public FixedResumeEvidenceAuditEvaluationService.Report evaluate() {
        return evaluationService.evaluate();
    }
}
