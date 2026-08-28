package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
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

import java.util.List;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/search")
@Tag(name = "知识检索", description = "基于向量相似度检索知识片段")
public class MarkdownSearchController {

    private final MarkdownSimilaritySearchService searchService;

    public MarkdownSearchController(MarkdownSimilaritySearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "检索知识片段", description = "按问题召回知识片段并返回来源、章节和相似度")
    public MarkdownSearchResponse search(@RequestParam String query,
                                         @RequestParam(defaultValue = "5") int topK,
                                         @RequestParam(defaultValue = "0.0") double similarityThreshold) {
        List<MarkdownSearchResult> results = searchService.search(query, topK, similarityThreshold);
        return MarkdownSearchResponse.from(query, topK, similarityThreshold, results);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
