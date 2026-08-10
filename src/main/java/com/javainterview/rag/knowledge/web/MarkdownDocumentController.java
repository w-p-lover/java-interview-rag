package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
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
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/documents")
public class MarkdownDocumentController {

    private final MarkdownContentImporter contentImporter;
    private final MarkdownImportRegistry importRegistry;

    public MarkdownDocumentController(MarkdownContentImporter contentImporter,
                                      MarkdownImportRegistry importRegistry) {
        this.contentImporter = contentImporter;
        this.importRegistry = importRegistry;
    }

    @GetMapping
    public KnowledgeDocumentCatalogResponse findCurrentDocuments() {
        return KnowledgeDocumentCatalogResponse.from(importRegistry.findCurrentDocuments());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarkdownImportResponse importDocument(@RequestParam MultipartFile file,
                                                 @RequestParam String source) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        MarkdownImportResult result = contentImporter.importContent(source, content);
        return MarkdownImportResponse.from(result);
    }

    @PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarkdownRebuildResponse rebuildDocument(@RequestParam MultipartFile file,
                                                    @RequestParam String source) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return MarkdownRebuildResponse.from(contentImporter.rebuildContent(source, content));
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
}
