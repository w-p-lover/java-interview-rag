package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewQuestionControllerTest {

    @Test
    void returnsGeneratedQuestionsWithTraceableKnowledgeSources() throws Exception {
        InterviewQuestionGenerationService service = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = new InterviewPreparationProfile(
                "具备 Java RAG 项目经验。",
                List.of("使用 Qdrant 实现向量检索"),
                List.of(),
                List.of(new com.javainterview.rag.interview.preparation.InterviewFocusArea(
                        "RAG 工程化",
                        "目标岗位和简历均涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME)))
        );
        MarkdownSearchResult source = new MarkdownSearchResult(
                "文档更新后需要重建向量。",
                "rag/vector-lifecycle.md",
                "version-1",
                0,
                List.of("RAG", "向量生命周期"),
                0.88
        );
        InterviewQuestion question = new InterviewQuestion(
                "文档更新后如何保证向量一致性？",
                "验证向量生命周期治理能力",
                List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME),
                "RAG 文档更新 向量一致性",
                true,
                List.of(source)
        );
        when(service.generate(eq("Java AI 应用开发工程师"), eq(profile)))
                .thenReturn(new InterviewQuestionGenerationResult(
                        "Java AI 应用开发工程师",
                        "interview-question-v1",
                        List.of(question, question, question)
                ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewQuestionController(service)).build();

        mockMvc.perform(post("/api/interviews/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":"Java AI 应用开发工程师",
                                  "profile":{
                                    "candidateSummary":"具备 Java RAG 项目经验。",
                                    "resumeEvidence":["使用 Qdrant 实现向量检索"],
                                    "jobRequirements":[],
                                    "focusAreas":[{
                                      "topic":"RAG 工程化",
                                      "reason":"目标岗位和简历均涉及 RAG",
                                      "sources":["TARGET_ROLE","RESUME"]
                                    }]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptVersion").value("interview-question-v1"))
                .andExpect(jsonPath("$.questions[0].question").value("文档更新后如何保证向量一致性？"))
                .andExpect(jsonPath("$.questions[0].basisSources[1]").value("RESUME"))
                .andExpect(jsonPath("$.questions[0].knowledgeGrounded").value(true))
                .andExpect(jsonPath("$.questions[0].knowledgeSources[0].source")
                        .value("rag/vector-lifecycle.md"))
                .andExpect(jsonPath("$.questions[0].knowledgeSources[0].similarity").value(0.88));
    }

    @Test
    void returnsBadRequestForInvalidQuestionInput() throws Exception {
        InterviewQuestionGenerationService service = mock(InterviewQuestionGenerationService.class);
        when(service.generate(eq(" "), org.mockito.ArgumentMatchers.any(InterviewPreparationProfile.class)))
                .thenThrow(new IllegalArgumentException("目标岗位不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewQuestionController(service)).build();

        mockMvc.perform(post("/api/interviews/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":" ",
                                  "profile":{
                                    "candidateSummary":"Java 后端候选人",
                                    "resumeEvidence":["Spring Boot 项目"],
                                    "jobRequirements":[],
                                    "focusAreas":[{
                                      "topic":"Spring Boot",
                                      "reason":"简历包含项目经验",
                                      "sources":["RESUME"]
                                    }]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("目标岗位不能为空"));
    }
}
