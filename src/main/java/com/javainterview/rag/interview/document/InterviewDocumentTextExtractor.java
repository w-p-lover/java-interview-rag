package com.javainterview.rag.interview.document;

import org.springframework.web.multipart.MultipartFile;

public interface InterviewDocumentTextExtractor {

    InterviewDocumentExtraction extract(MultipartFile file);
}
