package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.JavaInterviewRagApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JavaInterviewRagApplication.class)
@AutoConfigureMockMvc
class MarkdownDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void importsMultipartMarkdownAndRejectsDuplicateVersion() throws Exception {
        MockMultipartFile file = markdownFile("""
                # Java
                第一段

                第二段
                ## 集合
                HashMap
                """);

        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(file)
                        .param("source", "java/web-basic.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("java/web-basic.md"))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.imported").value(true))
                .andExpect(jsonPath("$.chunkCount").value(2))
                .andExpect(jsonPath("$.chunks[0].order").value(0))
                .andExpect(jsonPath("$.chunks[0].headingPath[0]").value("Java"))
                .andExpect(jsonPath("$.chunks[1].headingPath[1]").value("集合"));

        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(file)
                        .param("source", "java/web-basic.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(false))
                .andExpect(jsonPath("$.chunkCount").value(0))
                .andExpect(jsonPath("$.chunks").isEmpty());
    }

    @Test
    void returnsBadRequestForBlankSource() throws Exception {
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(markdownFile("# Java\n内容"))
                        .param("source", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档来源不能为空"));

        mockMvc.perform(delete("/api/knowledge/documents")
                        .param("source", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档来源不能为空"));

        mockMvc.perform(delete("/api/knowledge/documents")
                        .param("source", "java/basic.md "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档来源不能包含首尾空白"));
    }

    @Test
    void deletesTheDocumentSourceAndAllowsItToBeImportedAgain() throws Exception {
        MockMultipartFile file = markdownFile("# Java\n删除后重试");
        String source = "java/web-delete.md";
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(file)
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(true));

        mockMvc.perform(delete("/api/knowledge/documents")
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value(source))
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(file)
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(true));
    }

    private MockMultipartFile markdownFile(String content) {
        return new MockMultipartFile("file", "basic.md", "text/markdown",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
