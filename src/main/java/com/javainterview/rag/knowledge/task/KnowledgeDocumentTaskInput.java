package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownSection;

import java.util.List;
import java.util.Objects;

public record KnowledgeDocumentTaskInput(
        String fileId,
        String source,
        String content,
        String format,
        Integer pageCount,
        List<Integer> pagesNeedingOcr,
        List<String> warnings,
        List<MarkdownSection> sections
) {

    public KnowledgeDocumentTaskInput {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("文件 ID 不能为空");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        content = Objects.requireNonNull(content, "资料正文不能为空");
        format = Objects.requireNonNull(format, "资料格式不能为空");
        pagesNeedingOcr = List.copyOf(Objects.requireNonNull(pagesNeedingOcr, "OCR 页码不能为空"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "解析告警不能为空"));
        sections = List.copyOf(Objects.requireNonNull(sections, "结构化章节不能为空"));
    }

    public boolean structured() {
        return "PDF".equals(format);
    }
}
