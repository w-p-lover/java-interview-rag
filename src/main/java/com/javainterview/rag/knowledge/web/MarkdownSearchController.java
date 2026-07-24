package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
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
public class MarkdownSearchController {

    private final MarkdownSimilaritySearchService searchService;

    public MarkdownSearchController(MarkdownSimilaritySearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
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
