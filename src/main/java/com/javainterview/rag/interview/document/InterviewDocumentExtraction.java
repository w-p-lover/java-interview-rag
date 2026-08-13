package com.javainterview.rag.interview.document;

import java.util.List;

public record InterviewDocumentExtraction(
        String content,
        String format,
        int pageCount,
        PdfDocumentType documentType,
        int textPageCount,
        int imagePageCount,
        int reorderedPageCount,
        List<Integer> pagesNeedingOcr,
        List<String> warnings,
        List<PdfPageExtraction> pages) {

    public InterviewDocumentExtraction {
        pagesNeedingOcr = List.copyOf(pagesNeedingOcr);
        warnings = List.copyOf(warnings);
        pages = List.copyOf(pages);
    }

    public InterviewDocumentExtraction(String content, String format, int pageCount) {
        this(content, format, pageCount, PdfDocumentType.TEXT_BASED, pageCount, 0, 0,
                List.of(), List.of(), List.of());
    }
}
