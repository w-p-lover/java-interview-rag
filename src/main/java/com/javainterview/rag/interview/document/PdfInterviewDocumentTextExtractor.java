package com.javainterview.rag.interview.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PdfInterviewDocumentTextExtractor implements InterviewDocumentTextExtractor {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final PdfDocumentAnalyzer analyzer = new PdfDocumentAnalyzer();

    @Override
    public InterviewDocumentExtraction extract(MultipartFile file) {
        validateFile(file);
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            List<PdfPageAnalysis> pages = analyzer.analyze(document);
            String content = pages.stream()
                    .map(PdfPageAnalysis::content)
                    .filter(pageContent -> !pageContent.isBlank())
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElse("")
                    .trim();
            if (content.isEmpty()) {
                throw new IllegalArgumentException("未提取到可用文字，可能是扫描件；当前暂不支持 OCR");
            }
            return buildExtraction(content, pages);
        } catch (InvalidPasswordException exception) {
            throw new IllegalArgumentException("PDF 已加密，当前无法提取正文，请先解除密码保护");
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF 解析失败，请确认文件完整");
        }
    }

    private InterviewDocumentExtraction buildExtraction(String content, List<PdfPageAnalysis> pages) {
        int textPageCount = (int) pages.stream().filter(page -> page.textCharacterCount() > 0).count();
        int imagePageCount = (int) pages.stream().filter(page -> page.imageCount() > 0).count();
        int reorderedPageCount = (int) pages.stream()
                .filter(page -> page.layout() == PdfPageLayout.TWO_COLUMN)
                .count();
        List<Integer> pagesNeedingOcr = pages.stream()
                .filter(PdfPageAnalysis::requiresOcr)
                .map(PdfPageAnalysis::pageNumber)
                .toList();
        List<String> warnings = new ArrayList<>();
        if (!pagesNeedingOcr.isEmpty()) {
            warnings.add("第 " + joinPageNumbers(pagesNeedingOcr)
                    + " 页未提取到文字，可能需要 OCR；请检查或手动补充正文");
        }
        if (reorderedPageCount > 0) {
            warnings.add("已按双栏版面调整 " + reorderedPageCount + " 页阅读顺序，请在开始面试前核对正文");
        }
        return new InterviewDocumentExtraction(
                content,
                "PDF",
                pages.size(),
                documentType(pages),
                textPageCount,
                imagePageCount,
                reorderedPageCount,
                pagesNeedingOcr,
                warnings,
                pages.stream().map(PdfPageExtraction::from).toList());
    }

    private PdfDocumentType documentType(List<PdfPageAnalysis> pages) {
        boolean hasText = pages.stream().anyMatch(page -> page.textCharacterCount() > 0);
        boolean hasPageNeedingOcr = pages.stream().anyMatch(PdfPageAnalysis::requiresOcr);
        boolean hasImage = pages.stream().anyMatch(page -> page.imageCount() > 0);
        if (hasText && hasPageNeedingOcr) {
            return PdfDocumentType.MIXED;
        }
        if (hasText) {
            return PdfDocumentType.TEXT_BASED;
        }
        if (hasImage) {
            return PdfDocumentType.IMAGE_BASED;
        }
        return PdfDocumentType.EMPTY;
    }

    private String joinPageNumbers(List<Integer> pageNumbers) {
        return pageNumbers.stream().map(String::valueOf).reduce((left, right) -> left + "、" + right).orElse("");
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
