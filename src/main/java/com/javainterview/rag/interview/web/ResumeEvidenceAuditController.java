package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/resume-audits")
public class ResumeEvidenceAuditController {

    private final ResumeEvidenceAuditService auditService;
    private final ResumeEvidenceAuditRequestLogQueryService logQueryService;

    public ResumeEvidenceAuditController(
            ResumeEvidenceAuditService auditService,
            ResumeEvidenceAuditRequestLogQueryService logQueryService) {
        this.auditService = auditService;
        this.logQueryService = logQueryService;
    }

    @PostMapping
    public ResumeEvidenceAuditResult audit(@RequestBody ResumeEvidenceAuditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("简历证据审计请求不能为空");
        }
        return auditService.audit(request.resumeText());
    }

    @GetMapping("/requests")
    public List<ResumeEvidenceAuditRequestLog> findRecentRequests(
            @RequestParam(defaultValue = "20") int limit) {
        return logQueryService.findRecent(limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleAuditFailure(IllegalStateException exception) {
        return Map.of("message", exception.getMessage());
    }
}
