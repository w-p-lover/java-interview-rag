package com.javainterview.rag.interview.tool;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

public class InterviewReviewTools {

    private final InterviewReviewTaskService reviewTaskService;

    public InterviewReviewTools(InterviewReviewTaskService reviewTaskService) {
        this.reviewTaskService = Objects.requireNonNull(reviewTaskService, "复习任务服务不能为空");
    }

    @Tool(
            name = "list_review_tasks",
            description = "查询单个面试会话内已经生成的复习任务。支持按 PENDING/COMPLETED 状态和计划复习日期范围筛选。只返回任务摘要和知识来源元数据，不返回回答正文或知识正文。"
    )
    public ReviewTaskListToolResult listReviewTasks(
            @ToolParam(description = "面试会话 ID") String sessionId,
            @ToolParam(required = false, description = "复习任务状态，可选 PENDING 或 COMPLETED；为空返回全部") String status,
            @ToolParam(required = false, description = "计划复习开始日期，格式 yyyy-MM-dd；为空不限制") String plannedDateFrom,
            @ToolParam(required = false, description = "计划复习结束日期，格式 yyyy-MM-dd；为空不限制") String plannedDateTo
    ) {
        String normalizedSessionId = requireText(sessionId, "面试会话 ID 不能为空");
        String normalizedStatus = blankToNull(status);
        LocalDate from = parseOptionalDate(plannedDateFrom);
        LocalDate to = parseOptionalDate(plannedDateTo);
        List<InterviewReviewTask> tasks = reviewTaskService.findBySessionId(
                normalizedSessionId, normalizedStatus, from, to);
        return ReviewTaskListToolResult.from(normalizedSessionId, normalizedStatus, from, to, tasks);
    }

    private static LocalDate parseOptionalDate(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("计划复习日期格式必须为 yyyy-MM-dd");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static String requireText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    public record ReviewTaskListToolResult(
            String sessionId,
            String statusFilter,
            LocalDate plannedDateFrom,
            LocalDate plannedDateTo,
            int totalTaskCount,
            int pendingTaskCount,
            int completedTaskCount,
            List<ReviewTaskToolItem> tasks
    ) {

        static ReviewTaskListToolResult from(String sessionId,
                                             String statusFilter,
                                             LocalDate plannedDateFrom,
                                             LocalDate plannedDateTo,
                                             List<InterviewReviewTask> tasks) {
            List<ReviewTaskToolItem> items = tasks.stream()
                    .map(ReviewTaskToolItem::from)
                    .toList();
            int pendingTaskCount = (int) tasks.stream()
                    .filter(task -> task.status() == InterviewReviewTaskStatus.PENDING)
                    .count();
            int completedTaskCount = (int) tasks.stream()
                    .filter(task -> task.status() == InterviewReviewTaskStatus.COMPLETED)
                    .count();
            return new ReviewTaskListToolResult(
                    sessionId, statusFilter, plannedDateFrom, plannedDateTo,
                    items.size(), pendingTaskCount, completedTaskCount, items);
        }
    }

    public record ReviewTaskToolItem(
            String taskId,
            int questionIndex,
            String question,
            String objective,
            List<String> gaps,
            String improvementSuggestion,
            boolean knowledgeGrounded,
            List<ReviewTaskKnowledgeSourceToolItem> knowledgeSources,
            InterviewReviewTaskStatus status,
            LocalDate plannedReviewDate
    ) {

        static ReviewTaskToolItem from(InterviewReviewTask task) {
            return new ReviewTaskToolItem(
                    task.id(),
                    task.questionIndex(),
                    task.question(),
                    task.objective(),
                    task.gaps(),
                    task.improvementSuggestion(),
                    task.knowledgeGrounded(),
                    task.knowledgeSources().stream()
                            .map(ReviewTaskKnowledgeSourceToolItem::from)
                            .toList(),
                    task.status(),
                    task.plannedReviewDate()
            );
        }
    }

    public record ReviewTaskKnowledgeSourceToolItem(
            String source,
            String documentVersion,
            int chunkOrder,
            List<String> headingPath,
            Double similarity
    ) {

        static ReviewTaskKnowledgeSourceToolItem from(InterviewReviewKnowledgeSource source) {
            return new ReviewTaskKnowledgeSourceToolItem(
                    source.source(),
                    source.documentVersion(),
                    source.chunkOrder(),
                    source.headingPath(),
                    source.similarity()
            );
        }
    }
}
