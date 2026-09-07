package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagConversationTurn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "知识库问答请求")
public record RagAnswerRequest(
        @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "什么是 JVM 的双亲委派？")
        String question,
        @Schema(description = "当前页面最近几轮问答上下文，最多 4 轮")
        List<RagConversationTurn> history) {
}
