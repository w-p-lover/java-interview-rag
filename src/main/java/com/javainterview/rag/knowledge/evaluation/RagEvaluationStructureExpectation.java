package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;

import java.util.List;
import java.util.Objects;

public record RagEvaluationStructureExpectation(
        String source,
        List<String> headingPath,
        int pageNumber,
        KnowledgeBlockType blockType
) {

    public RagEvaluationStructureExpectation {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("结构期望来源不能为空");
        }
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "结构期望标题路径不能为空"));
        if (headingPath.isEmpty() || headingPath.stream().anyMatch(heading -> heading == null || heading.isBlank())) {
            throw new IllegalArgumentException("结构期望标题路径不能为空");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("结构期望页码必须从 1 开始");
        }
        Objects.requireNonNull(blockType, "结构期望内容块类型不能为空");
    }
}
