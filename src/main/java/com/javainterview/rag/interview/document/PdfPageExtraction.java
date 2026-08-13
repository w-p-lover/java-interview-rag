package com.javainterview.rag.interview.document;

public record PdfPageExtraction(
        int pageNumber,
        PdfPageContentType contentType,
        PdfPageLayout layout,
        int textCharacterCount,
        int imageCount,
        boolean requiresOcr) {

    static PdfPageExtraction from(PdfPageAnalysis analysis) {
        return new PdfPageExtraction(
                analysis.pageNumber(),
                analysis.contentType(),
                analysis.layout(),
                analysis.textCharacterCount(),
                analysis.imageCount(),
                analysis.requiresOcr());
    }
}
