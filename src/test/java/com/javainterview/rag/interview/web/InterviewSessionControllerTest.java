package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewSessionControllerTest {

    @Test
    void createsRestoresAndAdvancesTheInterviewSession() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        InterviewSession initial = session();
        InterviewSession answered = initial.answer(
                "使用稳定向量 ID，并在新版本写入成功后清理旧版本。",
                LocalDateTime.of(2026, 7, 27, 11, 5));
        when(service.start(initial.targetRole(), initial.profile(), initial.questions())).thenReturn(initial);
        when(service.find("session-1")).thenReturn(initial);
        when(service.submitAnswer("session-1", "使用稳定向量 ID，并在新版本写入成功后清理旧版本。"))
                .thenReturn(answered);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewSessionController(service)).build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewSessionCreateRequest(
                                initial.targetRole(), initial.profile(), initial.questions()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentQuestionIndex").value(0))
                .andExpect(jsonPath("$.questions.length()").value(3));

        mockMvc.perform(get("/api/interviews/sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.candidateSummary")
                        .value("具备 Java 后端和 RAG 项目经验。"));

        mockMvc.perform(post("/api/interviews/sessions/session-1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"使用稳定向量 ID，并在新版本写入成功后清理旧版本。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestionIndex").value(1))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.answers[0].questionIndex").value(0));
    }

    @Test
    void mapsInvalidMissingAndConflictingSessionsToHttpStatus() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        when(service.start(" ", profile(), questions()))
                .thenThrow(new IllegalArgumentException("目标岗位不能为空"));
        when(service.find("missing"))
                .thenThrow(new NoSuchElementException("面试会话不存在"));
        when(service.submitAnswer("completed", "额外回答"))
                .thenThrow(new InterviewSessionConflictException("面试会话已经完成"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InterviewSessionController(service)).build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InterviewSessionCreateRequest(" ", profile(), questions()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("目标岗位不能为空"));

        mockMvc.perform(get("/api/interviews/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("面试会话不存在"));

        mockMvc.perform(post("/api/interviews/sessions/completed/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"额外回答\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("面试会话已经完成"));
    }

    private InterviewSession session() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 11, 0);
        return new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
                profile(),
                questions(),
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                now,
                now
        );
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发项目"),
                List.of("岗位要求 MySQL 调优"),
                List.of(new InterviewFocusArea(
                        "RAG 工程化",
                        "目标岗位和简历涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME)))
        );
    }

    private List<InterviewQuestion> questions() {
        return List.of(
                question("如何保证向量更新一致性？", "向量生命周期"),
                question("如何定位 MySQL 慢查询？", "MySQL 调优"),
                question("模型调用失败如何治理？", "AI 工程治理")
        );
    }

    private InterviewQuestion question(String text, String objective) {
        return new InterviewQuestion(
                text,
                objective,
                List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME),
                objective,
                false,
                List.of()
        );
    }
}
