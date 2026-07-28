package com.javainterview.rag.interview.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Component
public class PdfInterviewDocumentTextExtractor implements InterviewDocumentTextExtractor {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    @Override
    public InterviewDocumentExtraction extract(MultipartFile file) {
        validateFile(file);
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            textStripper.setPageEnd("\n\n");
            String content = textStripper.getText(document).trim();
            if (content.isEmpty()) {
                throw new IllegalArgumentException("未提取到可用文字，可能是扫描件；当前暂不支持 OCR");
            }
            return new InterviewDocumentExtraction(content, "PDF", document.getNumberOfPages());
        } catch (InvalidPasswordException exception) {
            throw new IllegalArgumentException("PDF 已加密，当前无法提取正文，请先解除密码保护");
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF 解析失败，请确认文件完整");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF 文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("PDF 文件不能超过 10 MiB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("当前仅支持 PDF 文件");
        }
    }
}
