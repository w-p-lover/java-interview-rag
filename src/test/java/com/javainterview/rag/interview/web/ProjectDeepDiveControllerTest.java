package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveResult;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveService;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectDeepDiveControllerTest {

    @Test
    void returnsProjectDeepDiveContract() throws Exception {
        ProjectDeepDiveService service = mock(ProjectDeepDiveService.class);
        when(service.generate(any())).thenReturn(new ProjectDeepDiveResult(
                "project-deep-dive-v1",
                new ProjectDeepDiveResult.Project("负责知识库问答", "Java Interview RAG"),
                false,
                List.of(new ProjectDeepDiveResult.Question(
                        ProjectDeepDiveResult.Dimension.FOUNDATION,
                        "请介绍项目背景",
                        "确认项目背景",
                        "Java Interview RAG",
                        "Java Interview RAG 项目背景",
                        false,
                        List.of()))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProjectDeepDiveController(
                service, mock(ResumeAnalysisResultService.class))).build();

        mockMvc.perform(post("/api/interviews/project-deep-dives")
                        .contentType("application/json")
                        .content("""
                                {
                                  "project": {
                                    "type": "PROJECT",
                                    "statement": "负责知识库问答",
                                    "evidence": "Java Interview RAG"
                                  },
                                  "includeKnowledge": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyVersion").value("project-deep-dive-v1"))
                .andExpect(jsonPath("$.project.evidence").value("Java Interview RAG"))
                .andExpect(jsonPath("$.questions[0].dimension").value("FOUNDATION"))
                .andExpect(jsonPath("$.questions[0].knowledgeGrounded").value(false));
    }

    @Test
    void mapsInvalidProjectFactToBadRequest() throws Exception {
        ProjectDeepDiveService service = mock(ProjectDeepDiveService.class);
        when(service.generate(any())).thenThrow(new IllegalArgumentException("项目深挖只能使用 PROJECT 类型的已确认事实"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProjectDeepDiveController(
                service, mock(ResumeAnalysisResultService.class))).build();

        mockMvc.perform(post("/api/interviews/project-deep-dives")
                        .contentType("application/json")
                        .content("""
                                {
                                  "project": {
                                    "type": "TECHNOLOGY",
                                    "statement": "使用 Spring AI",
                                    "evidence": "Spring AI"
                                  },
                                  "includeKnowledge": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("项目深挖只能使用 PROJECT 类型的已确认事实"));
    }
}
