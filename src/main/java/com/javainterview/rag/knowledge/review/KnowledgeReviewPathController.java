package com.javainterview.rag.knowledge.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/review-path")
@Tag(name = "知识复习路径", description = "按复习回答表现生成确定性复习推荐")
public class KnowledgeReviewPathController {
    private final KnowledgeReviewPathService service;

    public KnowledgeReviewPathController(KnowledgeReviewPathService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询知识复习路径")
    public KnowledgeReviewPathResult recommend() {
        return service.recommend();
    }
}
