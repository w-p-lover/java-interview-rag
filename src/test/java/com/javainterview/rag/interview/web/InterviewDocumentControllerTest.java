package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.document.InterviewDocumentExtraction;
import com.javainterview.rag.interview.document.InterviewDocumentTextExtractor;
import com.javainterview.rag.interview.document.PdfDocumentType;
import com.javainterview.rag.interview.document.PdfPageContentType;
import com.javainterview.rag.interview.document.PdfPageExtraction;
import com.javainterview.rag.interview.document.PdfPageLayout;
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
                "Java 后端项目经历", "PDF", 2,
                PdfDocumentType.MIXED, 1, 1, 1,
                java.util.List.of(2),
                java.util.List.of("第 2 页未提取到文字，可能需要 OCR；请检查或手动补充正文"),
                java.util.List.of(
                        new PdfPageExtraction(1, PdfPageContentType.TEXT, PdfPageLayout.TWO_COLUMN,
                                8, 0, false),
                        new PdfPageExtraction(2, PdfPageContentType.IMAGE_ONLY, PdfPageLayout.SINGLE_COLUMN,
                                0, 1, true))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewDocumentController(extractor)).build();

        mockMvc.perform(multipart("/api/interviews/documents/extract")
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "PDF".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Java 后端项目经历"))
                .andExpect(jsonPath("$.format").value("PDF"))
                .andExpect(jsonPath("$.pageCount").value(2))
                .andExpect(jsonPath("$.documentType").value("MIXED"))
                .andExpect(jsonPath("$.textPageCount").value(1))
                .andExpect(jsonPath("$.imagePageCount").value(1))
                .andExpect(jsonPath("$.reorderedPageCount").value(1))
                .andExpect(jsonPath("$.pagesNeedingOcr[0]").value(2))
                .andExpect(jsonPath("$.pages[0].layout").value("TWO_COLUMN"))
                .andExpect(jsonPath("$.pages[1].requiresOcr").value(true));
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
