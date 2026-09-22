package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.bank.BankGenerationService;
import com.javainterview.rag.interview.bank.BankGenerationTask;
import com.javainterview.rag.interview.bank.QuestionBankService;
import com.javainterview.rag.interview.bank.QuestionBankStore;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.question.InterviewSkillTrainingCatalog;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionBankControllerTest {

    @Test
    void submitsGenerationTaskAndMapsSessionConflict() throws Exception {
        QuestionBankService bank = mock(QuestionBankService.class);
        BankGenerationService generation = mock(BankGenerationService.class);
        QuestionBankStore store = mock(QuestionBankStore.class);
        BankGenerationTask task = task();
        when(generation.submit("request-1", "java.md", InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE, 3, 1)).thenReturn(task);
        when(bank.startSession(any())).thenThrow(
                new InterviewSessionConflictException("可用题目不足"));
        when(bank.trainingPlan(InterviewSkill.JAVA_CORE, 3, "并发与边界"))
                .thenReturn(new InterviewSkillTrainingCatalog().plan(
                        InterviewSkill.JAVA_CORE, 3, "并发与边界"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new QuestionBankController(bank, generation, store)).build();

        mockMvc.perform(post("/api/interviews/question-bank/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"request-1","source":"java.md","skill":"JAVA_CORE",
                                 "difficulty":"INTERMEDIATE","questionCount":3,"followUpCount":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"))
                .andExpect(jsonPath("$.recoveryCount").value(0))
                .andExpect(jsonPath("$.status").value("QUEUED"));
        verify(generation).submit("request-1", "java.md", InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE, 3, 1);

        mockMvc.perform(post("/api/interviews/question-bank/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"session-1","source":"java.md","questionIds":[],
                                 "questionCount":3,"followUpCount":0,"skill":"JAVA_CORE",
                                 "difficulty":"INTERMEDIATE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("可用题目不足"));

        mockMvc.perform(get("/api/interviews/question-bank/training-plan")
                        .param("skill", "JAVA_CORE")
                        .param("questionCount", "3")
                        .param("focusDimension", "并发与边界"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("interview-skill-training-v1"))
                .andExpect(jsonPath("$.focusDimension").value("并发与边界"))
                .andExpect(jsonPath("$.categories[1].category").value("并发与边界"))
                .andExpect(jsonPath("$.categories[1].questionCount").value(2));
    }

    @Test
    void cancelsQueuedTaskAndRejectsRunningTask() throws Exception {
        BankGenerationService generation = mock(BankGenerationService.class);
        BankGenerationTask cancelled = task().progress(BankGenerationTask.Status.CANCELLED, 0, 0, null, null);
        when(generation.cancel("task-1")).thenReturn(cancelled);
        when(generation.cancel("running-task")).thenThrow(
                new InterviewSessionConflictException("仅排队中的题库任务可以取消，运行或已结束的任务保持原状态"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new QuestionBankController(
                mock(QuestionBankService.class), generation, mock(QuestionBankStore.class))).build();

        mockMvc.perform(patch("/api/interviews/question-bank/tasks/task-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(patch("/api/interviews/question-bank/tasks/running-task/cancel"))
                .andExpect(status().isConflict());
    }

    private BankGenerationTask task() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 11, 0);
        return new BankGenerationTask("task-1", "request-1", "java.md", "v1",
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE, 3, 1,
                BankGenerationTask.Status.QUEUED, 0, 0, null,
                null, null, null, 0,
                "knowledge-question-bank-v2", "test-model", null, 0, now, now);
    }
}
