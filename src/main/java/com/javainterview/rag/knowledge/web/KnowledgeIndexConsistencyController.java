package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.consistency.KnowledgeIndexConsistencyReport;
import com.javainterview.rag.knowledge.consistency.KnowledgeIndexRepairResult;
import com.javainterview.rag.knowledge.consistency.KnowledgeIndexConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/documents/consistency")
@Tag(name = "知识索引巡检", description = "核对 MySQL 登记、Qdrant 与 Lucene 索引一致性")
public class KnowledgeIndexConsistencyController {

    private final KnowledgeIndexConsistencyService service;

    public KnowledgeIndexConsistencyController(KnowledgeIndexConsistencyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "巡检单个知识来源", description = "只执行精确计数，不调用 Embedding 或修改索引")
    public KnowledgeIndexConsistencyReport inspect(@RequestParam String source) {
        return service.inspect(source);
    }

    @PostMapping("/repair")
    @Operation(summary = "按当前版本快照修复索引",
            description = "仅在人工确认后重建当前版本 Dense/Sparse 索引，并返回修复后的巡检结果")
    public KnowledgeIndexRepairResult repair(@RequestParam String source) {
        return service.repair(source);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingDocument(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }
}
