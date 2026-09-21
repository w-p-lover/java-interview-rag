package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import com.javainterview.rag.interview.resume.ResumeAnalysisType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/resumes/versions/{resumeVersionId}/analyses")
@Tag(name = "简历版本分析结果", description = "按不可变简历版本恢复成功的分析结果")
public class ResumeAnalysisResultController {

    private final ResumeAnalysisResultService service;

    public ResumeAnalysisResultController(ResumeAnalysisResultService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询版本的全部分析结果")
    public List<ResumeAnalysisResultService.ResultView> findAll(
            @PathVariable String resumeVersionId) {
        return service.findAll(resumeVersionId);
    }

    @GetMapping("/{analysisType}")
    @Operation(summary = "查询版本的最近一次指定分析结果")
    public ResumeAnalysisResultService.ResultView findLatest(
            @PathVariable String resumeVersionId,
            @PathVariable ResumeAnalysisType analysisType) {
        return service.findLatest(resumeVersionId, analysisType);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingResult(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }
}
