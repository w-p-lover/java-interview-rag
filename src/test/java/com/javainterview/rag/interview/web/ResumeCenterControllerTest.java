package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.resume.ResumeCenterService;
import com.javainterview.rag.interview.resume.ResumeVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeCenterControllerTest {

    private ResumeCenterService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ResumeCenterService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ResumeCenterController(service)).build();
    }

    @Test
    void uploadsVersionAndReturnsDuplicateState() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "简历正文".getBytes());
        ResumeCenterService.ResumeDetail detail = detail();
        when(service.save(eq(file), eq(null), eq("Java 简历"),
                eq("Java 后端工程师"), eq(true)))
                .thenReturn(new ResumeCenterService.SaveResult(detail, "version-1", false));

        mvc.perform(multipart("/api/resumes/versions")
                        .file(file)
                        .param("name", "Java 简历")
                        .param("targetRole", "Java 后端工程师")
                        .param("saveFullText", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedVersionId").value("version-1"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.detail.currentVersionId").value("version-1"));
    }

    @Test
    void listsDetailsAndSwitchesCurrentVersion() throws Exception {
        ResumeCenterService.ResumeDetail detail = detail();
        when(service.list()).thenReturn(List.of(new ResumeCenterService.ResumeSummary(
                "resume-1", "Java 简历", "Java 后端工程师", "version-1",
                0, 1, 1, true, detail.createdAt(), detail.updatedAt())));
        when(service.detail("resume-1")).thenReturn(detail);
        when(service.switchCurrent("resume-1", "version-1", 0)).thenReturn(detail);

        mvc.perform(get("/api/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionCount").value(1));
        mvc.perform(get("/api/resumes/resume-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[0].fullTextSaved").value(true))
                .andExpect(jsonPath("$.versions[0].fullText").doesNotExist());
        mvc.perform(patch("/api/resumes/resume-1/current-version")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsBytes(
                                new ResumeCenterController.SwitchCurrentVersionRequest("version-1", 0))))
                .andExpect(status().isOk());

        verify(service).switchCurrent("resume-1", "version-1", 0);
    }

    @Test
    void mapsInvalidInputAndVersionConflicts() throws Exception {
        when(service.detail("missing")).thenThrow(new java.util.NoSuchElementException("简历记录不存在"));
        when(service.switchCurrent("resume-1", "version-2", 0))
                .thenThrow(new com.javainterview.rag.interview.resume.ResumeCenterConflictException(
                        "简历记录已更新，请刷新后重试"));

        mvc.perform(get("/api/resumes/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("简历记录不存在"));
        mvc.perform(patch("/api/resumes/resume-1/current-version")
                        .contentType("application/json")
                        .content("{\"versionId\":\"version-2\",\"expectedVersion\":0}"))
                .andExpect(status().isConflict());
    }

    private ResumeCenterService.ResumeDetail detail() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 9, 30);
        ResumeVersion version = new ResumeVersion(
                "version-1", "resume-1", 1,
                "a".repeat(64), "resume.txt", "text/plain", 12,
                "简历正文", now);
        return new ResumeCenterService.ResumeDetail(
                "resume-1", "Java 简历", "Java 后端工程师", "version-1",
                0, now, now, List.of(version));
    }
}
