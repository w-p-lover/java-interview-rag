package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixedInterviewAnswerEvaluationDataset {

    private static final InterviewPreparationProfile PROFILE = new InterviewPreparationProfile(
            "具备 Java 后端和 RAG 项目经验。",
            List.of("使用 Spring Boot 和 Qdrant 开发知识库"),
            List.of("岗位要求掌握向量生命周期和故障治理"),
            List.of(new InterviewFocusArea(
                    "RAG 工程化",
                    "简历与岗位都涉及 RAG 工程实践",
                    List.of(InterviewFocusSource.RESUME, InterviewFocusSource.TARGET_ROLE))));

    private static final List<Case> CASES = List.of(
            new Case("blank-answer", PROFILE, groundedQuestion(), "", 0, 0, true, true),
            new Case("irrelevant-answer", PROFILE, groundedQuestion(),
                    "这个问题我不清楚，我平时主要负责页面开发。",
                    0, 45, false, true),
            new Case("partial-grounded-answer", PROFILE, groundedQuestion(),
                    "先写入新版本，再删除旧版本。",
                    35, 85, false, true),
            new Case("complete-grounded-answer", PROFILE, groundedQuestion(),
                    "新版本全部写入成功后，按来源和版本删除旧向量；chunk 使用稳定 ID，"
                            + "清理失败时保留登记并通过重建接口重试，直到存储收敛。",
                    65, 100, false, true),
            new Case("ungrounded-answer", PROFILE, ungroundedQuestion(),
                    "我会先记录线程池活跃数、队列长度和拒绝次数，再结合线程栈定位线程泄漏。",
                    30, 90, false, false)
    );

    public List<Case> findAll() {
        return CASES;
    }

    private static InterviewQuestion groundedQuestion() {
        return new InterviewQuestion(
                "如何保证向量更新最终一致？",
                "向量生命周期",
                List.of(InterviewFocusSource.RESUME, InterviewFocusSource.TARGET_ROLE),
                "向量更新最终一致",
                true,
                List.of(new MarkdownSearchResult(
                        "新版本成功写入后再清理旧版本；失败时使用稳定 ID 重试并通过重建收敛。",
                        "rag/vector-lifecycle.md",
                        "baseline-v1",
                        0,
                        List.of("RAG", "向量生命周期"),
                        1.0)));
    }

    private static InterviewQuestion ungroundedQuestion() {
        return new InterviewQuestion(
                "如何定位线程泄漏？",
                "Java 故障排查",
                List.of(InterviewFocusSource.TARGET_ROLE),
                "线程泄漏排查",
                false,
                List.of());
    }

    public record Case(
            String id,
            InterviewPreparationProfile profile,
            InterviewQuestion question,
            String answer,
            int expectedMinOverallScore,
            int expectedMaxOverallScore,
            boolean expectedRejected,
            boolean expectedKnowledgeGrounded
    ) {

        public Case {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("评分样本标识不能为空");
            }
            if (profile == null || question == null || answer == null) {
                throw new IllegalArgumentException("评分样本内容不能为空");
            }
            if (expectedMinOverallScore < 0 || expectedMaxOverallScore > 100
                    || expectedMinOverallScore > expectedMaxOverallScore) {
                throw new IllegalArgumentException("评分样本预期分数范围无效");
            }
            if (!expectedRejected && answer.isBlank()) {
                throw new IllegalArgumentException("非拒绝样本的回答不能为空");
            }
        }
    }
}
