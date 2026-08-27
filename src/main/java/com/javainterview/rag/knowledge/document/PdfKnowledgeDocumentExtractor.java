package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.interview.document.PdfDocumentAnalyzer;
import com.javainterview.rag.interview.document.PdfPageAnalysis;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 将文本型 PDF 转为现有 Markdown 导入链路可处理的页面结构文本。
 */
public final class PdfKnowledgeDocumentExtractor {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final PdfDocumentAnalyzer analyzer;
    private final PdfKnowledgeStructureParser structureParser;

    public PdfKnowledgeDocumentExtractor() {
        this(new PdfDocumentAnalyzer(), new PdfKnowledgeStructureParser());
    }

    PdfKnowledgeDocumentExtractor(PdfDocumentAnalyzer analyzer,
                                  PdfKnowledgeStructureParser structureParser) {
        this.analyzer = Objects.requireNonNull(analyzer, "PDF 页面分析器不能为空");
        this.structureParser = Objects.requireNonNull(structureParser, "PDF 结构解析器不能为空");
    }

    public PdfKnowledgeDocumentExtraction extract(MultipartFile file) {
        validateFile(file);
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            List<PdfPageAnalysis> analyses = analyzer.analyze(document);
            List<PdfKnowledgePage> pages = analyses.stream()
                    .map(page -> new PdfKnowledgePage(page.pageNumber(), page.content(), page.contentType(),
                            page.layout(), page.requiresOcr()))
                    .toList();
            List<Integer> pagesNeedingOcr = pages.stream()
                    .filter(PdfKnowledgePage::requiresOcr)
                    .map(PdfKnowledgePage::pageNumber)
                    .toList();
            List<String> warnings = buildWarnings(pagesNeedingOcr, pages);
            List<PdfKnowledgeBlock> blocks = structureParser.parse(analyses);
            String markdownContent = structureParser.toMarkdown(blocks);
            return new PdfKnowledgeDocumentExtraction(markdownContent, pages.size(), pagesNeedingOcr,
                    warnings, pages, blocks);
        } catch (InvalidPasswordException exception) {
            throw new IllegalArgumentException("PDF 已加密，当前无法提取正文，请先解除密码保护");
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF 解析失败，请确认文件完整");
        }
    }

    public List<MarkdownSection> toSections(String source, List<PdfKnowledgeBlock> blocks) {
        return structureParser.toSections(source, blocks);
    }

    private List<String> buildWarnings(List<Integer> pagesNeedingOcr, List<PdfKnowledgePage> pages) {
        List<String> warnings = new ArrayList<>();
        if (!pagesNeedingOcr.isEmpty()) {
            warnings.add("第 " + pagesNeedingOcr.stream().map(String::valueOf).collect(Collectors.joining("、"))
                    + " 页未提取到文字，可能需要 OCR；这些页面不会进入知识库");
        }
        long reorderedPages = pages.stream()
                .filter(page -> page.layout() == com.javainterview.rag.interview.document.PdfPageLayout.TWO_COLUMN)
                .count();
        if (reorderedPages > 0) {
            warnings.add("已按双栏版面调整 " + reorderedPages + " 页阅读顺序，请检查章节结构");
        }
        return warnings;
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
