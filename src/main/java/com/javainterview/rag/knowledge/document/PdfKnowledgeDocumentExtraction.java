package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

public record PdfKnowledgeDocumentExtraction(String markdownContent, int pageCount,
                                             List<Integer> pagesNeedingOcr,
                                             List<String> warnings,
                                             List<PdfKnowledgePage> pages) {

    public PdfKnowledgeDocumentExtraction {
        Objects.requireNonNull(markdownContent, "PDF 转换正文不能为空");
        if (markdownContent.isBlank()) {
            throw new IllegalArgumentException("PDF 未提取到可用文字，可能是扫描件；当前暂不支持 OCR");
        }
        if (pageCount < 1) {
            throw new IllegalArgumentException("PDF 页数必须大于 0");
        }
        pagesNeedingOcr = List.copyOf(Objects.requireNonNull(pagesNeedingOcr, "OCR 候选页不能为空"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "PDF 解析告警不能为空"));
        pages = List.copyOf(Objects.requireNonNull(pages, "PDF 页面不能为空"));
    }
}
