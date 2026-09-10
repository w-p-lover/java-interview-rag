package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
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

/** local profile 使用进程内 Lucene 索引，保持前端查资料入口可用且不依赖 Qdrant。 */
@RestController
@Profile("!storage")
@RequestMapping("/api/knowledge/search")
@Tag(name = "知识检索", description = "local 使用 Lucene BM25 检索知识片段")
public class LocalMarkdownSearchController {

    private final MarkdownSparseSearchService searchService;

    public LocalMarkdownSearchController(MarkdownSparseSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "检索知识片段", description = "local 使用 BM25 返回来源、章节和稀疏检索分数")
    public MarkdownSearchResponse search(@RequestParam String query,
                                         @RequestParam(defaultValue = "5") int topK,
                                         @RequestParam(defaultValue = "0.0") double similarityThreshold) {
        if (!Double.isFinite(similarityThreshold) || similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("相似度阈值必须在 0 到 1 之间");
        }
        List<MarkdownSearchResult> results = searchService.search(query, topK);
        return MarkdownSearchResponse.from(query, topK, similarityThreshold, results);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
