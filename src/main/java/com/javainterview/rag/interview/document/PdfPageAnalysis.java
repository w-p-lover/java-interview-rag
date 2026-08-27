package com.javainterview.rag.interview.document;

public record PdfPageAnalysis(
        int pageNumber,
        String content,
        int textCharacterCount,
        int imageCount,
        PdfPageContentType contentType,
        PdfPageLayout layout,
        float width,
        float height,
        java.util.List<PdfTextLineAnalysis> lines) {

    public PdfPageAnalysis {
        lines = java.util.List.copyOf(lines);
    }

    public boolean requiresOcr() {
        return contentType == PdfPageContentType.IMAGE_ONLY;
    }
}
