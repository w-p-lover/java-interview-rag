package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/knowledge/search/sparse")
@Tag(name = "知识稀疏检索", description = "基于 Lucene BM25 的关键词检索")
public class MarkdownSparseSearchController {

    private final MarkdownSparseSearchService searchService;

    public MarkdownSparseSearchController(MarkdownSparseSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "BM25 稀疏检索", description = "按关键词召回知识片段，适合类名、方法名、异常名和数字阈值")
    public MarkdownSparseSearchResponse search(@RequestParam String query,
                                               @RequestParam(defaultValue = "5") int topK) {
        List<MarkdownSearchResult> results = searchService.search(query, topK);
        return MarkdownSparseSearchResponse.from(query, topK, results);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
