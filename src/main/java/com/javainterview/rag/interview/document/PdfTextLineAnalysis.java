package com.javainterview.rag.interview.document;

import java.util.Objects;

public record PdfTextLineAnalysis(String text, float xStart, float xEnd, float y,
                                  float fontSize, boolean bold, boolean monospaced,
                                  int visualRow, int visualRowSegmentCount) {

    public PdfTextLineAnalysis {
        Objects.requireNonNull(text, "PDF 文本行不能为空");
        if (fontSize < 0) {
            throw new IllegalArgumentException("PDF 字体大小不能小于 0");
        }
        if (visualRow < 0) {
            throw new IllegalArgumentException("PDF 视觉行号不能小于 0");
        }
        if (visualRowSegmentCount < 1) {
            throw new IllegalArgumentException("PDF 视觉行分段数必须大于 0");
        }
    }
}
