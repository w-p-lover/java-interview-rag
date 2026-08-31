package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "开始项目深挖面试请求")
public record ProjectDeepDiveSessionStartRequest(
        @Schema(description = "目标岗位", example = "Java 后端开发工程师")
        String targetRole,
        @Schema(description = "已由简历证据审计确认的项目事实", requiredMode = Schema.RequiredMode.REQUIRED)
        ProjectDeepDiveRequest.ConfirmedProject project,
        @Schema(description = "是否只读检索知识库补充技术背景")
        boolean includeKnowledge
) {

    public ProjectDeepDiveRequest toDeepDiveRequest() {
        return new ProjectDeepDiveRequest(project, includeKnowledge);
    }
}
