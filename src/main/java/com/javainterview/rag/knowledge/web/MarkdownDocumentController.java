package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtraction;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Locale;

@RestController
@RequestMapping("/api/knowledge/documents")
public class MarkdownDocumentController {

    private final MarkdownContentImporter contentImporter;
    private final MarkdownImportRegistry importRegistry;
    private final PdfKnowledgeDocumentExtractor pdfExtractor;

    public MarkdownDocumentController(MarkdownContentImporter contentImporter,
                                      MarkdownImportRegistry importRegistry,
                                      PdfKnowledgeDocumentExtractor pdfExtractor) {
        this.contentImporter = contentImporter;
        this.importRegistry = importRegistry;
        this.pdfExtractor = pdfExtractor;
    }

    @GetMapping
    public KnowledgeDocumentCatalogResponse findCurrentDocuments() {
        return KnowledgeDocumentCatalogResponse.from(importRegistry.findCurrentDocuments());
    }

    @GetMapping("/detail")
    public KnowledgeDocumentDetailsResponse findDocumentDetails(@RequestParam String source) {
        validateSource(source);
        List<KnowledgeDocumentSummary> versions = importRegistry.findVersionsBySource(source);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("资料不存在");
        }
        return KnowledgeDocumentDetailsResponse.from(versions);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarkdownImportResponse importDocument(@RequestParam MultipartFile file,
                                                 @RequestParam String source) throws IOException {
        KnowledgeUploadContent upload = readContent(file, source);
        MarkdownImportResult result = upload.structured()
                ? contentImporter.importStructuredContent(source, upload.content(), upload.sections())
                : contentImporter.importContent(source, upload.content());
        return MarkdownImportResponse.from(result, upload.format(), upload.pageCount(),
                upload.pagesNeedingOcr(), upload.warnings());
    }

    @PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarkdownRebuildResponse rebuildDocument(@RequestParam MultipartFile file,
                                                    @RequestParam String source) throws IOException {
        KnowledgeUploadContent upload = readContent(file, source);
        MarkdownRebuildResult result = upload.structured()
                ? contentImporter.rebuildStructuredContent(source, upload.content(), upload.sections())
                : contentImporter.rebuildContent(source, upload.content());
        return MarkdownRebuildResponse.from(result,
                upload.format(), upload.pageCount(), upload.pagesNeedingOcr(), upload.warnings());
    }

    @DeleteMapping
    public MarkdownDeleteResponse deleteDocument(@RequestParam String source) {
        return new MarkdownDeleteResponse(source, contentImporter.deleteBySource(source));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingDocument(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    private void validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
    }

    private KnowledgeUploadContent readContent(MultipartFile file, String source) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("资料文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            PdfKnowledgeDocumentExtraction extraction = pdfExtractor.extract(file);
            return new KnowledgeUploadContent(extraction.markdownContent(), "PDF",
                    extraction.pageCount(), extraction.pagesNeedingOcr(), extraction.warnings(),
                    pdfExtractor.toSections(source, extraction.blocks()));
        }
        return new KnowledgeUploadContent(new String(file.getBytes(), StandardCharsets.UTF_8),
                "MARKDOWN", null, List.of(), List.of(), List.of());
    }

    private record KnowledgeUploadContent(String content, String format, Integer pageCount,
                                          List<Integer> pagesNeedingOcr, List<String> warnings,
                                          List<MarkdownSection> sections) {

        private boolean structured() {
            return "PDF".equals(format);
        }
    }
}
