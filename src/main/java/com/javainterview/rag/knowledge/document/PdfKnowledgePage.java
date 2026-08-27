package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.interview.document.PdfPageContentType;
import com.javainterview.rag.interview.document.PdfPageLayout;

import java.util.Objects;

public record PdfKnowledgePage(int pageNumber, String content, PdfPageContentType contentType,
                               PdfPageLayout layout, boolean requiresOcr) {

    public PdfKnowledgePage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("PDF 页码必须从 1 开始");
        }
        Objects.requireNonNull(content, "PDF 页面正文不能为空");
        Objects.requireNonNull(contentType, "PDF 页面类型不能为空");
        Objects.requireNonNull(layout, "PDF 页面版式不能为空");
    }
}
