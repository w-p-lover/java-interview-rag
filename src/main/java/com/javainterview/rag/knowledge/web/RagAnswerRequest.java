package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库问答请求")
public record RagAnswerRequest(
        @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "什么是 JVM 的双亲委派？")
        String question) {
}
