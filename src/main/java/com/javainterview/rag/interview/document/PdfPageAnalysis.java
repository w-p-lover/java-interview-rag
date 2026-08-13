package com.javainterview.rag.interview.document;

record PdfPageAnalysis(
        int pageNumber,
        String content,
        int textCharacterCount,
        int imageCount,
        PdfPageContentType contentType,
        PdfPageLayout layout) {

    boolean requiresOcr() {
        return contentType == PdfPageContentType.IMAGE_ONLY;
    }
}
