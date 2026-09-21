package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveRequest;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveResult;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveService;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import com.javainterview.rag.interview.resume.ResumeAnalysisType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/project-deep-dives")
@Tag(name = "项目深挖", description = "基于已确认项目事实生成四维深挖题")
public class ProjectDeepDiveController {

    private final ProjectDeepDiveService service;
    private final ResumeAnalysisResultService analysisResultService;

    public ProjectDeepDiveController(
            ProjectDeepDiveService service,
            ResumeAnalysisResultService analysisResultService) {
        this.service = service;
        this.analysisResultService = analysisResultService;
    }

    @PostMapping
    @Operation(summary = "生成项目深挖题", description = "固定覆盖基础、取舍、故障和扩展；知识库只作为可选答题背景")
    public ProjectDeepDiveResult generate(@RequestBody ProjectDeepDiveRequest request) {
        if (request == null || request.resumeVersionId() == null || request.resumeVersionId().isBlank()) {
            return service.generate(request);
        }
        return analysisResultService.execute(
                request.resumeVersionId(), ResumeAnalysisType.PROJECT_DEEP_DIVE,
                new DeepDiveFingerprint(request.project(), request.includeKnowledge()),
                service.strategyVersion(), ResumeAnalysisResultService.LOCAL_MODEL,
                () -> service.generate(request));
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

    private record DeepDiveFingerprint(
            ProjectDeepDiveRequest.ConfirmedProject project,
            boolean includeKnowledge) { }
}
