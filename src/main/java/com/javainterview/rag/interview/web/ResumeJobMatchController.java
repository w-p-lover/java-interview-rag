package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.matching.ResumeJobMatchRequest;
import com.javainterview.rag.interview.matching.ResumeJobMatchResult;
import com.javainterview.rag.interview.matching.ResumeJobMatchService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLog;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import com.javainterview.rag.interview.resume.ResumeAnalysisType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/job-matches")
@Tag(name = "岗位匹配", description = "基于已确认简历事实分析岗位要求覆盖情况")
public class ResumeJobMatchController {

    private final ResumeJobMatchService matchService;
    private final ResumeJobMatchRequestLogQueryService logQueryService;
    private final ResumeAnalysisResultService analysisResultService;

    public ResumeJobMatchController(ResumeJobMatchService matchService,
                                    ResumeJobMatchRequestLogQueryService logQueryService,
                                    ResumeAnalysisResultService analysisResultService) {
        this.matchService = matchService;
        this.logQueryService = logQueryService;
        this.analysisResultService = analysisResultService;
    }

    @PostMapping
    @Operation(summary = "分析岗位匹配", description = "只使用岗位描述和已确认简历事实，不接收原始简历")
    public ResumeJobMatchResult match(@RequestBody ResumeJobMatchRequest request) {
        if (request == null || request.resumeVersionId() == null || request.resumeVersionId().isBlank()) {
            return matchService.match(request);
        }
        return analysisResultService.execute(
                request.resumeVersionId(), ResumeAnalysisType.JOB_MATCH,
                new JobMatchFingerprint(
                        request.targetRole(), request.jobDescription(), request.confirmedFacts()),
                matchService.promptVersion(), matchService.modelName(),
                () -> matchService.match(request));
    }

    @GetMapping("/requests")
    @Operation(summary = "查询岗位匹配请求", description = "仅返回模型、token、耗时和失败类型等元数据")
    public java.util.List<ResumeJobMatchRequestLog> findRecentRequests(
            @RequestParam(defaultValue = "20") int limit) {
        return logQueryService.findRecent(limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingVersion(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleMatchFailure(IllegalStateException exception) {
        return Map.of("message", exception.getMessage());
    }

    private record JobMatchFingerprint(
            String targetRole,
            String jobDescription,
            java.util.List<ResumeJobMatchRequest.ConfirmedFact> confirmedFacts) { }
}
