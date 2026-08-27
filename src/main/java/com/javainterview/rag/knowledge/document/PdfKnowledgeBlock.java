package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record PdfKnowledgeBlock(int pageNumber, KnowledgeBlockType type, int headingLevel,
                                List<String> headingPath, String content) {

    public PdfKnowledgeBlock {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("PDF 页码必须从 1 开始");
        }
        Objects.requireNonNull(type, "PDF 内容块类型不能为空");
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("PDF 标题级别必须在 0 到 6 之间");
        }
        headingPath = List.copyOf(Objects.requireNonNull(headingPath, "PDF 标题路径不能为空"));
        Objects.requireNonNull(content, "PDF 内容块正文不能为空");
        if (content.isBlank()) {
            throw new IllegalArgumentException("PDF 内容块正文不能为空");
        }
    }

    public boolean heading() {
        return headingLevel > 0;
    }
}
