package com.javainterview.rag.config;

import com.javainterview.rag.JavaInterviewRagApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JavaInterviewRagApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesDocumentedCoreApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Java 面试知识库与智能训练平台 API"))
                .andExpect(jsonPath("$.tags[*].name", hasItem("知识资料")))
                .andExpect(jsonPath("$.tags[*].name", hasItem("面试资料")))
                .andExpect(jsonPath("$.paths['/api/knowledge/documents'].get.summary")
                        .value("查询当前知识资料"));

        mockMvc.perform(get("/doc.html"))
                .andExpect(status().isOk());
    }
}
