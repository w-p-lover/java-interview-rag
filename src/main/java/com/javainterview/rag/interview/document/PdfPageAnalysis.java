package com.javainterview.rag.interview.document;

public record PdfPageAnalysis(
        int pageNumber,
        String content,
        int textCharacterCount,
        int imageCount,
        PdfPageContentType contentType,
        PdfPageLayout layout) {

    public boolean requiresOcr() {
        return contentType == PdfPageContentType.IMAGE_ONLY;
    }
}
