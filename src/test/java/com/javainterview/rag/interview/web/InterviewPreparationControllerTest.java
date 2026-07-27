package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewPreparationControllerTest {

    @Test
    void returnsTheGeneratedInterviewPreparationProfile() throws Exception {
        InterviewPreparationService service = mock(InterviewPreparationService.class);
        when(service.prepare("Java AI 应用开发工程师", "Java RAG 项目简历", "需要 MySQL 和 RAG"))
                .thenReturn(new InterviewPreparationResult(
                        "Java AI 应用开发工程师", "interview-preparation-v1",
                        new InterviewPreparationProfile(
                                "具备 Java 和 RAG 项目经验。",
                                List.of("实现过 Java RAG 项目"),
                                List.of("岗位需要 MySQL 和 RAG"),
                                List.of(new InterviewFocusArea(
                                        "RAG 工程化", "目标岗位、简历和 JD 均涉及 RAG",
                                        List.of(InterviewFocusSource.TARGET_ROLE,
                                                InterviewFocusSource.RESUME,
                                                InterviewFocusSource.JOB_DESCRIPTION)))
                        )));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewPreparationController(service)).build();

        mockMvc.perform(post("/api/interviews/preparations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":"Java AI 应用开发工程师",
                                  "resumeText":"Java RAG 项目简历",
                                  "jobDescription":"需要 MySQL 和 RAG"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole").value("Java AI 应用开发工程师"))
                .andExpect(jsonPath("$.promptVersion").value("interview-preparation-v1"))
                .andExpect(jsonPath("$.profile.resumeEvidence[0]").value("实现过 Java RAG 项目"))
                .andExpect(jsonPath("$.profile.focusAreas[0].topic").value("RAG 工程化"))
                .andExpect(jsonPath("$.profile.focusAreas[0].sources[0]").value("TARGET_ROLE"))
                .andExpect(jsonPath("$.profile.focusAreas[0].sources[2]").value("JOB_DESCRIPTION"));
    }

    @Test
    void returnsBadRequestForInvalidPreparationInput() throws Exception {
        InterviewPreparationService service = mock(InterviewPreparationService.class);
        when(service.prepare("Java 后端", " ", null))
                .thenThrow(new IllegalArgumentException("简历内容不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewPreparationController(service)).build();

        mockMvc.perform(post("/api/interviews/preparations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"Java 后端\",\"resumeText\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("简历内容不能为空"));
    }
}
