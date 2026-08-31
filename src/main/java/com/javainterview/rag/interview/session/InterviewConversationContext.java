package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.question.InterviewQuestion;

import java.util.Objects;

/** 从当前会话生成有界上下文，避免把完整简历和历史正文反复发送给模型。 */
public final class InterviewConversationContext {

    private static final int MAX_RECENT_ANSWERS = 6;

    private InterviewConversationContext() {
    }

    public static String from(InterviewSession session) {
        Objects.requireNonNull(session, "面试会话不能为空");
        if (session.answers().isEmpty()) {
            return "（这是本场面试的第一题）";
        }
        int start = Math.max(0, session.answers().size() - MAX_RECENT_ANSWERS);
        StringBuilder context = new StringBuilder();
        for (int index = start; index < session.answers().size(); index++) {
            InterviewSessionAnswer answer = session.answers().get(index);
            InterviewQuestion question = session.questions().get(answer.questionIndex());
            context.append("[第 ").append(answer.questionIndex() + 1).append(" 题摘要]\n")
                    .append("题目：").append(question.question()).append('\n')
                    .append("回答摘要：").append(summarize(answer.answer())).append('\n')
                    .append("评分：").append(answer.evaluation().overallScore()).append("；遗漏：")
                    .append(String.join("；", answer.evaluation().gaps())).append("\n\n");
        }
        return context.toString().strip();
    }

    private static String summarize(String answer) {
        String normalized = answer.replaceAll("\\s+", " ").strip();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "…";
    }
}
