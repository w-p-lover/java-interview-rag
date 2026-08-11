package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.JavaInterviewRagApplication;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/knowledge/documents/detail")
                        .param("source", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档来源不能为空"));
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

    @Test
    void rebuildsARegisteredMultipartMarkdownVersion() throws Exception {
        MockMultipartFile file = markdownFile("# Java\n重建内容");
        String source = "java/web-rebuild.md";
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(file)
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(true));

        mockMvc.perform(multipart("/api/knowledge/documents/rebuild")
                        .file(file)
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value(source))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.chunkCount").value(1));
    }

    @Test
    void rejectsRebuildForAnUnregisteredMultipartMarkdownVersion() throws Exception {
        mockMvc.perform(multipart("/api/knowledge/documents/rebuild")
                        .file(markdownFile("# Java\n未导入"))
                        .param("source", "java/web-missing.md"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档版本尚未导入，不能重建"));
    }

    @Test
    void listsTheCurrentVersionForEachDocumentSource() throws Exception {
        String source = "java/web-catalog.md";
        String content = "# Java\n目录内容";
        MarkdownDocument document = MarkdownDocument.create(source, content);
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(markdownFile(content))
                        .param("source", source))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/knowledge/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCount").isNumber())
                .andExpect(jsonPath("$.documents[?(@.source == '%s')].version".formatted(source))
                        .value(hasItem(document.version())))
                .andExpect(jsonPath("$.documents[?(@.source == '%s')].contentLength".formatted(source))
                        .value(hasItem(content.length())))
                .andExpect(jsonPath("$.documents[?(@.source == '%s')].registeredAt".formatted(source))
                        .isNotEmpty());
    }

    @Test
    void returnsCurrentAndHistoricalVersionDetails() throws Exception {
        String source = "java/web-detail.md";
        String originalContent = "# Java\n原内容";
        String currentContent = "# Java\n新内容";
        MarkdownDocument original = MarkdownDocument.create(source, originalContent);
        MarkdownDocument current = MarkdownDocument.create(source, currentContent);
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(markdownFile(originalContent))
                        .param("source", source))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/knowledge/documents/import")
                        .file(markdownFile(currentContent))
                        .param("source", source))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/knowledge/documents/detail")
                        .param("source", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value(source))
                .andExpect(jsonPath("$.currentVersion").value(current.version()))
                .andExpect(jsonPath("$.versionCount").value(2))
                .andExpect(jsonPath("$.versions[0].version").value(current.version()))
                .andExpect(jsonPath("$.versions[0].contentLength").value(currentContent.length()))
                .andExpect(jsonPath("$.versions[0].current").value(true))
                .andExpect(jsonPath("$.versions[1].version").value(original.version()))
                .andExpect(jsonPath("$.versions[1].current").value(false));
    }

    @Test
    void returnsNotFoundForMissingDocumentDetails() throws Exception {
        mockMvc.perform(get("/api/knowledge/documents/detail")
                        .param("source", "java/missing-detail.md"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资料不存在"));
    }

    private MockMultipartFile markdownFile(String content) {
        return new MockMultipartFile("file", "basic.md", "text/markdown",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
