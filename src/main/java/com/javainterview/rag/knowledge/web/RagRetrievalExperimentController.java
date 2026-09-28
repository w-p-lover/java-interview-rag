package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluation-experiments/retrieval")
@Tag(name = "RAG 检索实验", description = "持久化运行、恢复、比较并记录采用结论")
public class RagRetrievalExperimentController {

    private final RagRetrievalExperimentService service;

    public RagRetrievalExperimentController(RagRetrievalExperimentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "启动或复用同契约检索实验")
    public ResponseEntity<RagRetrievalExperimentService.RunView> start(
            @RequestBody RagRetrievalExperimentStartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("检索实验请求不能为空");
        }
        return ResponseEntity.accepted().body(service.start(
                request.name(), request.strategy(), request.split()));
    }

    @GetMapping
    @Operation(summary = "查询最近检索实验")
    public List<RagRetrievalExperimentService.RunView> recent(
            @RequestParam(defaultValue = "20") int limit) {
        return service.findRecent(limit);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "查询检索实验详情")
    public RagRetrievalExperimentService.RunView find(@PathVariable String runId) {
        return service.find(runId);
    }

    @PostMapping("/{runId}/resume")
    @Operation(summary = "恢复检索实验，仅补未成功样本")
    public ResponseEntity<RagRetrievalExperimentService.RunView> resume(
            @PathVariable String runId) {
        return ResponseEntity.accepted().body(service.resume(runId));
    }

    @GetMapping("/comparison")
    @Operation(summary = "比较同一数据与索引契约的基线和候选")
    public RagRetrievalExperimentService.Comparison compare(
            @RequestParam String baselineRunId,
            @RequestParam String candidateRunId) {
        return service.compare(baselineRunId, candidateRunId);
    }

    @PatchMapping("/{candidateRunId}/decision")
    @Operation(summary = "记录候选采用或回退结论")
    public RagRetrievalExperimentService.RunView decide(
            @PathVariable String candidateRunId,
            @RequestBody RagRetrievalExperimentDecisionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("实验决策请求不能为空");
        }
        return service.decide(candidateRunId, request.baselineRunId(),
                request.decision(), request.note());
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
