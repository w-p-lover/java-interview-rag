package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.document.InterviewDocumentExtraction;
import com.javainterview.rag.interview.document.InterviewDocumentTextExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/interviews/documents")
public class InterviewDocumentController {

    private final InterviewDocumentTextExtractor textExtractor;

    public InterviewDocumentController(InterviewDocumentTextExtractor textExtractor) {
        this.textExtractor = textExtractor;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewDocumentExtraction extract(@RequestParam MultipartFile file) {
        return textExtractor.extract(file);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleOversizedFile(MaxUploadSizeExceededException exception) {
        return Map.of("message", "PDF 文件不能超过 10 MiB");
    }
}
