package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.JavaInterviewRagApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JavaInterviewRagApplication.class)
@AutoConfigureMockMvc
class KnowledgeDocumentTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitsTaskAndEventuallyReportsSuccessWithoutDocumentBody() throws Exception {
        String taskId = mockMvc.perform(multipart("/api/knowledge/documents/import-tasks")
                        .file(new MockMultipartFile("file", "async.md", "text/markdown",
                                "# Java\n异步任务".getBytes(StandardCharsets.UTF_8)))
                        .param("source", "java/async-task.md"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\\\"taskId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        String status = "";
        for (int attempt = 0; attempt < 20; attempt++) {
            String body = mockMvc.perform(get("/api/knowledge/documents/tasks/{taskId}", taskId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            status = body.contains("\"status\":\"SUCCEEDED\"") ? "SUCCEEDED" : body;
            if ("SUCCEEDED".equals(status)) {
                assertThat(body).contains("\"chunkCount\":1");
                assertThat(body).doesNotContain("异步任务");
                break;
            }
            Thread.sleep(25);
        }
        assertThat(status).isEqualTo("SUCCEEDED");
    }

    @Test
    void returnsNotFoundForUnknownTask() throws Exception {
        mockMvc.perform(get("/api/knowledge/documents/tasks/{taskId}", "missing-task"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("知识资料任务不存在"));
    }

    @Test
    void rejectsUnsupportedFileBeforeSubmittingTask() throws Exception {
        mockMvc.perform(multipart("/api/knowledge/documents/import-tasks")
                        .file(new MockMultipartFile("file", "archive.zip", "application/zip",
                                new byte[]{'P', 'K', 3, 4}))
                        .param("source", "java/archive.zip"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前仅支持 Markdown、TXT 或 PDF 文件"));
    }

}
