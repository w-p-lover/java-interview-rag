package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    public MarkdownDocumentController(MarkdownContentImporter contentImporter) {
        this.contentImporter = contentImporter;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarkdownImportResponse importDocument(@RequestParam MultipartFile file,
                                                 @RequestParam String source) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        MarkdownImportResult result = contentImporter.importContent(source, content);
        return MarkdownImportResponse.from(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
