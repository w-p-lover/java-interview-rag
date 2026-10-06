package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveRequest;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveSessionStartService;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.interview.resume.ResumeCenterStore;
import com.javainterview.rag.interview.resume.ResumeVersion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStartService;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionSummary;
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
    void startsCreatesRestoresAndAdvancesTheInterviewSession() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        InterviewSessionStartService startService = mock(InterviewSessionStartService.class);
        ProjectDeepDiveSessionStartService projectDeepDiveStartService =
                mock(ProjectDeepDiveSessionStartService.class);
        ResumeCenterStore resumeCenterStore = mock(ResumeCenterStore.class);
        InterviewSession initial = session();
        List<InterviewQuestion> deepDiveQuestions = List.of(
                question("项目背景是什么？", "项目基础"),
                question("为何选择当前方案？", "技术取舍"),
                question("如何处理项目故障？", "故障复盘"),
                question("项目下一步如何演进？", "架构演进"));
        InterviewSession deepDive = new InterviewSession(
                initial.id(), initial.targetRole(), InterviewSessionMode.PROJECT_DEEP_DIVE,
                initial.profile(), deepDiveQuestions, deepDiveQuestions.size(), initial.answers(),
                initial.currentQuestionIndex(), initial.status(), initial.version(), initial.report(),
                initial.createdAt(), initial.updatedAt());
        InterviewSession answered = initial.answer(
                "使用稳定向量 ID，并在新版本写入成功后清理旧版本。",
                evaluation(), null, LocalDateTime.of(2026, 7, 27, 11, 5));
        when(startService.start(
                "Java AI 应用开发工程师", "简历正文", "岗位描述", 5, 8)).thenReturn(initial);
        when(service.start(
                initial.targetRole(), initial.profile(), initial.questions(), 8,
                initial.configuration(), "request-1")).thenReturn(initial);
        when(service.find("session-1")).thenReturn(initial);
        when(service.findRecent(20)).thenReturn(List.of(InterviewSessionSummary.from(answered)));
        when(service.submitAnswer(
                "session-1", 0, 0L,
                "使用稳定向量 ID，并在新版本写入成功后清理旧版本。"))
                .thenReturn(answered);
        ProjectDeepDiveRequest deepDiveRequest = new ProjectDeepDiveRequest(
                new ProjectDeepDiveRequest.ConfirmedProject(
                        ResumeEvidenceAuditResult.FactType.PROJECT,
                        "负责 Java 面试训练平台",
                        "Java Interview RAG"),
                false);
        when(projectDeepDiveStartService.start(
                "Java AI 应用开发工程师", deepDiveRequest)).thenReturn(deepDive);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewSessionController(
                        service, startService, projectDeepDiveStartService, resumeCenterStore)).build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc.perform(post("/api/interviews/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":"Java AI 应用开发工程师",
                                  "resumeText":"简历正文",
                                  "jobDescription":"岗位描述",
                                  "initialQuestionCount":5,
                                  "maxQuestionCount":8
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.maxQuestionCount").value(8));

        mockMvc.perform(post("/api/interviews/sessions/start-project-deep-dive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":"Java AI 应用开发工程师",
                                  "project":{
                                    "type":"PROJECT",
                                    "statement":"负责 Java 面试训练平台",
                                    "evidence":"Java Interview RAG"
                                  },
                                  "includeKnowledge":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PROJECT_DEEP_DIVE"))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.maxQuestionCount").value(4));

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewSessionCreateRequest(
                                initial.targetRole(), initial.profile(), initial.questions(), 8,
                                initial.configuration(), "request-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentQuestionIndex").value(0))
                .andExpect(jsonPath("$.configuration.strategyVersion").value("legacy-v1"))
                .andExpect(jsonPath("$.questions.length()").value(3));

        mockMvc.perform(get("/api/interviews/sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.candidateSummary")
                        .value("具备 Java 后端和 RAG 项目经验。"));

        mockMvc.perform(get("/api/interviews/sessions").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("session-1"))
                .andExpect(jsonPath("$[0].answeredQuestionCount").value(1))
                .andExpect(jsonPath("$[0].questionCount").value(3))
                .andExpect(jsonPath("$[0].overallAverageScore").doesNotExist());

        mockMvc.perform(post("/api/interviews/sessions/session-1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionIndex":0,
                                  "expectedVersion":0,
                                  "answer":"使用稳定向量 ID，并在新版本写入成功后清理旧版本。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestionIndex").value(1))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.answers[0].questionIndex").value(0))
                .andExpect(jsonPath("$.answers[0].evaluation.overallScore").value(80));
    }

    @Test
    void mapsInvalidMissingAndConflictingSessionsToHttpStatus() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        InterviewSessionStartService startService = mock(InterviewSessionStartService.class);
        ProjectDeepDiveSessionStartService projectDeepDiveStartService =
                mock(ProjectDeepDiveSessionStartService.class);
        ResumeCenterStore resumeCenterStore = mock(ResumeCenterStore.class);
        when(service.start(" ", profile(), questions(), 8))
                .thenThrow(new IllegalArgumentException("目标岗位不能为空"));
        when(service.find("missing"))
                .thenThrow(new NoSuchElementException("面试会话不存在"));
        when(service.submitAnswer("completed", 3, 3L, "额外回答"))
                .thenThrow(new InterviewSessionConflictException("面试会话已经完成"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewSessionController(
                        service, startService, projectDeepDiveStartService, resumeCenterStore)).build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InterviewSessionCreateRequest(" ", profile(), questions(), 8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("目标岗位不能为空"));

        mockMvc.perform(get("/api/interviews/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("面试会话不存在"));

        mockMvc.perform(post("/api/interviews/sessions/completed/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionIndex":3,"expectedVersion":3,"answer":"额外回答"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("面试会话已经完成"));
    }

    @Test
    void restoresEvaluationStartedStageForAnsweredSession() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        InterviewSessionStartService startService = mock(InterviewSessionStartService.class);
        ProjectDeepDiveSessionStartService projectDeepDiveStartService =
                mock(ProjectDeepDiveSessionStartService.class);
        ResumeCenterStore resumeCenterStore = mock(ResumeCenterStore.class);
        InterviewSession answered = session().answer(
                "说明向量更新流程。", evaluation(), null, LocalDateTime.of(2026, 7, 27, 11, 5));
        when(service.find("session-1")).thenReturn(answered);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewSessionController(
                        service, startService, projectDeepDiveStartService, resumeCenterStore)).build();

        mockMvc.perform(get("/api/interviews/sessions/session-1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ANSWER_SUBMITTED"))
                .andExpect(jsonPath("$[1].type").value("EVALUATION_STARTED"))
                .andExpect(jsonPath("$[2].type").value("EVALUATION_COMPLETED"))
                .andExpect(jsonPath("$[3].type").value("FOLLOW_UP_GENERATED"));
    }

    @Test
    void bindsExistingResumeVersionAndRejectsMissingVersion() throws Exception {
        InterviewSessionService service = mock(InterviewSessionService.class);
        InterviewSessionStartService startService = mock(InterviewSessionStartService.class);
        ProjectDeepDiveSessionStartService projectDeepDiveStartService =
                mock(ProjectDeepDiveSessionStartService.class);
        ResumeCenterStore resumeCenterStore = mock(ResumeCenterStore.class);
        InterviewSession initial = session();
        InterviewSession bound = new InterviewSession(
                initial.id(), initial.targetRole(), "resume-version-1", initial.mode(),
                initial.profile(), initial.configuration(), initial.questions(),
                initial.maxQuestionCount(), initial.answers(), initial.currentQuestionIndex(),
                initial.status(), initial.version(), initial.report(), initial.createdAt(),
                initial.updatedAt());
        ResumeVersion version = new ResumeVersion(
                "resume-version-1", "resume-1", 1, "a".repeat(64), "resume.txt",
                "text/plain", 10, null, LocalDateTime.of(2026, 9, 21, 8, 0));
        when(resumeCenterStore.findVersion("resume-version-1"))
                .thenReturn(java.util.Optional.of(version));
        when(resumeCenterStore.findVersion("missing-version"))
                .thenReturn(java.util.Optional.empty());
        when(service.start(
                initial.targetRole(), "resume-version-1", initial.profile(), initial.questions(),
                8, initial.configuration(), "request-bound")).thenReturn(bound);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewSessionController(
                        service, startService, projectDeepDiveStartService, resumeCenterStore)).build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewSessionCreateRequest(
                                initial.targetRole(), initial.profile(), initial.questions(), 8,
                                initial.configuration(), "request-bound", "resume-version-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeVersionId").value("resume-version-1"));

        mockMvc.perform(post("/api/interviews/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewSessionCreateRequest(
                                initial.targetRole(), initial.profile(), initial.questions(), 8,
                                initial.configuration(), "request-missing", "missing-version"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("绑定的简历版本不存在"));
    }

    private InterviewSession session() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 11, 0);
        return new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
                InterviewSessionMode.STANDARD,
                profile(),
                questions(),
                8,
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                null,
                now,
                now
        );
    }

    private InterviewAnswerEvaluation evaluation() {
        return new InterviewAnswerEvaluation(
                80, 70, 90, 80,
                List.of("说明了关键流程"),
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                true
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
