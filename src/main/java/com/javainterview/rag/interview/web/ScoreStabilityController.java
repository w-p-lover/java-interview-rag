package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.AnswerExperimentDecision;
import com.javainterview.rag.interview.evaluation.ScoreStabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/evaluation-experiments/score-stability")
@Tag(name = "评分稳定性实验", description = "固定回答重复评分的稳定性评估")
public class ScoreStabilityController {
    private final ScoreStabilityService service;
    public ScoreStabilityController(ScoreStabilityService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "启动或复用评分稳定性实验")
    public ResponseEntity<ScoreStabilityService.RunView> start(@RequestBody StartRequest request) {
        if (request == null) throw new IllegalArgumentException("评分稳定性实验请求不能为空");
        return ResponseEntity.accepted().body(service.start(request.name()));
    }
    @GetMapping
    public List<ScoreStabilityService.RunView> recent(@RequestParam(defaultValue = "20") int limit) {
        return service.recent(limit);
    }
    @GetMapping("/{runId}")
    public ScoreStabilityService.RunView find(@PathVariable String runId) { return service.find(runId); }
    @PostMapping("/{runId}/resume")
    public ResponseEntity<ScoreStabilityService.RunView> resume(@PathVariable String runId) {
        return ResponseEntity.accepted().body(service.resume(runId));
    }
    @GetMapping("/comparison")
    public ScoreStabilityService.Comparison compare(@RequestParam String baselineRunId,
                                                    @RequestParam String candidateRunId) {
        return service.compare(baselineRunId, candidateRunId);
    }
    @PatchMapping("/{candidateRunId}/decision")
    public ScoreStabilityService.RunView decide(@PathVariable String candidateRunId,
                                                @RequestBody DecisionRequest request) {
        if (request == null) throw new IllegalArgumentException("评分稳定性实验决策不能为空");
        return service.decide(candidateRunId, request.baselineRunId(), request.decision(), request.note());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) { return Map.of("message", exception.getMessage()); }
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) { return Map.of("message", exception.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(IllegalStateException exception) { return Map.of("message", exception.getMessage()); }

    public record StartRequest(String name) { }
    public record DecisionRequest(String baselineRunId, AnswerExperimentDecision decision, String note) { }
}
