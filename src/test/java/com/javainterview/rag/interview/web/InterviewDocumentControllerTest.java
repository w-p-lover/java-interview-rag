package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.document.InterviewDocumentExtraction;
import com.javainterview.rag.interview.document.InterviewDocumentTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewDocumentControllerTest {

    @Test
    void extractsPdfTextFromMultipartRequest() throws Exception {
        InterviewDocumentTextExtractor extractor = mock(InterviewDocumentTextExtractor.class);
        when(extractor.extract(any())).thenReturn(new InterviewDocumentExtraction(
                "Java 后端项目经历", "PDF", 2));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewDocumentController(extractor)).build();

        mockMvc.perform(multipart("/api/interviews/documents/extract")
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "PDF".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Java 后端项目经历"))
                .andExpect(jsonPath("$.format").value("PDF"))
                .andExpect(jsonPath("$.pageCount").value(2));
    }

    @Test
    void returnsBadRequestForInvalidPdf() throws Exception {
        InterviewDocumentTextExtractor extractor = mock(InterviewDocumentTextExtractor.class);
        when(extractor.extract(any())).thenThrow(new IllegalArgumentException("PDF 文件不能超过 10 MiB"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewDocumentController(extractor)).build();

        mockMvc.perform(multipart("/api/interviews/documents/extract")
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "PDF".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PDF 文件不能超过 10 MiB"));
    }

    @Test
    void exposesTheConfiguredLimitForMultipartRejection() {
        InterviewDocumentController controller = new InterviewDocumentController(mock(InterviewDocumentTextExtractor.class));

        assertEquals("PDF 文件不能超过 10 MiB",
                controller.handleOversizedFile(new MaxUploadSizeExceededException(10L * 1024 * 1024))
                        .get("message"));
    }
}
