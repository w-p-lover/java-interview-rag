package com.javainterview.rag.interview.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluations/resume-audits")
@Tag(name = "简历审计评估", description = "运行固定简历审计评估集")
public class ResumeEvidenceAuditEvaluationController {

    private final ResumeEvidenceAuditEvaluationRunService runService;

    public ResumeEvidenceAuditEvaluationController(
            ResumeEvidenceAuditEvaluationRunService runService) {
        this.runService = runService;
    }

    @PostMapping
    @Operation(summary = "启动或恢复当前契约的简历审计评估")
    public ResponseEntity<ResumeEvidenceAuditEvaluationRunService.RunView> start() {
        return ResponseEntity.accepted().body(runService.start());
    }

    @GetMapping("/{runId}")
    @Operation(summary = "查询简历审计评估运行状态和结果")
    public ResumeEvidenceAuditEvaluationRunService.RunView find(@PathVariable String runId) {
        return runService.find(runId);
    }

    @PostMapping("/{runId}/resume")
    @Operation(summary = "恢复简历审计评估运行，仅重试未成功样本")
    public ResponseEntity<ResumeEvidenceAuditEvaluationRunService.RunView> resume(
            @PathVariable String runId) {
        return ResponseEntity.accepted().body(runService.resume(runId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(IllegalStateException exception) {
        return Map.of("message", exception.getMessage());
    }
}
