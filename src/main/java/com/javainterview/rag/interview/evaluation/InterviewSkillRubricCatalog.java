package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.question.InterviewSkill;

import java.util.Objects;

public class InterviewSkillRubricCatalog {

    public static final String VERSION = "interview-skill-rubric-v1";

    public InterviewSkillRubric forSkill(InterviewSkill skill) {
        Objects.requireNonNull(skill, "面试 Skill 不能为空");
        return switch (skill) {
            case JAVA_CORE -> rubric(skill,
                    "原理与实现", "判断 Java 原理、关键实现机制和技术结论是否准确。",
                    "并发与边界", "检查并发竞争、异常路径、资源边界和恢复方式是否完整。",
                    "结论与推理表达", "检查是否先给结论，再用清晰因果链和示例支撑。");
            case SPRING -> rubric(skill,
                    "框架机制与实现", "判断 Spring 容器、代理、事务和工程实现说明是否准确。",
                    "事务与故障边界", "检查生命周期、事务失效、异常传播和恢复边界是否完整。",
                    "调用链表达", "检查是否按入口、框架机制和业务结果说明调用链。");
            case DATABASE -> rubric(skill,
                    "数据库机制与方案", "判断索引、事务、锁和执行计划相关结论是否准确。",
                    "一致性与性能边界", "检查并发一致性、容量、退化条件和排障验证是否完整。",
                    "证据化表达", "检查是否用执行计划、指标或数据变化支撑结论。");
            case SYSTEM_DESIGN -> rubric(skill,
                    "方案可行性", "判断架构职责、数据流和关键技术方案是否可落地。",
                    "容量、一致性与恢复", "检查容量估算、一致性取舍、故障信号和降级恢复是否完整。",
                    "取舍表达", "检查是否明确约束、备选方案、选择理由和代价。");
            case RAG_AI -> rubric(skill,
                    "检索生成正确性", "判断解析、召回、排序、生成和引用链路的技术结论是否准确。",
                    "边界、评估与恢复", "检查拒答、证据校验、质量评估、降级和失败恢复是否完整。",
                    "证据与取舍表达", "检查是否区分事实与推断，并用指标、证据和取舍支撑结论。");
            case GENERAL -> rubric(skill,
                    "技术准确性", "判断核心技术结论和实现说明是否准确。",
                    "完整性与边界", "检查关键步骤、异常边界、取舍和验证方式是否完整。",
                    "结论与推理表达", "检查是否结构清楚、结论明确且推理可核对。");
        };
    }

    private InterviewSkillRubric rubric(InterviewSkill skill,
                                         String technicalFocus,
                                         String technicalCriteria,
                                         String completenessFocus,
                                         String completenessCriteria,
                                         String communicationFocus,
                                         String communicationCriteria) {
        return new InterviewSkillRubric(
                VERSION, skill,
                technicalFocus, technicalCriteria,
                completenessFocus, completenessCriteria,
                communicationFocus, communicationCriteria);
    }
}
