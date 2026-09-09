package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
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
@Tag(name = "简历证据审计", description = "基于简历原文进行可追溯事实审计")
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
    @Operation(summary = "审计简历证据", description = "只返回可由简历原文逐字支持的事实、缺失信息、风险和建议")
    public ResumeEvidenceAuditResult audit(@RequestBody ResumeEvidenceAuditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("简历证据审计请求不能为空");
        }
        return auditService.audit(request.resumeText());
    }

    @GetMapping("/requests")
    @Operation(summary = "查询最近审计请求", description = "仅返回审计元数据，不返回简历正文")
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

    @ExceptionHandler({NonTransientAiException.class, TransientAiException.class,
            ResourceAccessException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleModelFailure(RuntimeException exception) {
        String detail = exception.getMessage();
        boolean unauthorized = detail != null
                && (detail.contains("HTTP 401") || detail.contains("HTTP 403"));
        return Map.of("message", unauthorized
                ? "模型服务鉴权失败，请检查 API Key 和模型权限配置"
                : "模型服务暂时不可用，请稍后重试");
    }
}
