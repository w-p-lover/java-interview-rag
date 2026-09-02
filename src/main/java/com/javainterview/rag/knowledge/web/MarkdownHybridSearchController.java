package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/search/hybrid")
@Tag(name = "知识混合检索", description = "Dense 与 BM25 双路召回并使用 RRF 融合")
public class MarkdownHybridSearchController {

    private final MarkdownHybridSearchService searchService;

    public MarkdownHybridSearchController(MarkdownHybridSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Dense + BM25 混合检索", description = "并行召回候选并按排名融合，返回每条结果的双路命中情况")
    public MarkdownHybridSearchService.MarkdownHybridSearchResult search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.0") double denseSimilarityThreshold) {
        return searchService.searchDetailed(query, topK, denseSimilarityThreshold);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
