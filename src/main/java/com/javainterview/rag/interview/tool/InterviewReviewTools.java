package com.javainterview.rag.interview.tool;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class InterviewReviewTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewReviewTools.class);
    private static final String LIST_REVIEW_TASKS = "list_review_tasks";
    private static final String SCHEDULE_REVIEW_TASK = "schedule_review_task";

    private final InterviewReviewTaskService reviewTaskService;
    private final InterviewToolExecutionLogStore executionLogStore;

    public InterviewReviewTools(InterviewReviewTaskService reviewTaskService,
                                InterviewToolExecutionLogStore executionLogStore) {
        this.reviewTaskService = Objects.requireNonNull(reviewTaskService, "复习任务服务不能为空");
        this.executionLogStore = Objects.requireNonNull(
                executionLogStore, "面试工具执行日志存储不能为空");
    }

    @Tool(
            name = "list_review_tasks",
            description = "查询单个面试会话内已经生成的复习任务。支持按 PENDING/COMPLETED 状态和计划复习日期范围筛选。只返回任务摘要和知识来源元数据，不返回回答正文或知识正文。"
    )
    public ReviewTaskListToolResult listReviewTasks(
            @ToolParam(description = "面试会话 ID") String sessionId,
            @ToolParam(required = false, description = "复习任务状态，可选 PENDING 或 COMPLETED；为空返回全部") String status,
            @ToolParam(required = false, description = "计划复习开始日期，格式 yyyy-MM-dd；为空不限制") String plannedDateFrom,
            @ToolParam(required = false, description = "计划复习结束日期，格式 yyyy-MM-dd；为空不限制") String plannedDateTo,
            ToolContext toolContext
    ) {
        String assistantRequestId = markToolExecution(toolContext);
        String normalizedSessionId = requireText(sessionId, "面试会话 ID 不能为空");
        return executeLogged(LIST_REVIEW_TASKS, assistantRequestId, normalizedSessionId, () -> {
            String normalizedStatus = blankToNull(status);
            LocalDate from = parseOptionalDate(plannedDateFrom);
            LocalDate to = parseOptionalDate(plannedDateTo);
            List<InterviewReviewTask> tasks = reviewTaskService.findBySessionId(
                    normalizedSessionId, normalizedStatus, from, to);
            return ReviewTaskListToolResult.from(
                    normalizedSessionId, normalizedStatus, from, to, tasks);
        }, ReviewTaskListToolResult::totalTaskCount);
    }

    @Tool(
            name = "schedule_review_task",
            description = "为单个面试会话中的指定复习任务设置明确的计划复习日期。必须同时提供会话 ID、任务 ID 和 yyyy-MM-dd 日期；重复设置同一日期保持幂等。"
    )
    public ReviewTaskScheduleToolResult scheduleReviewTask(
            @ToolParam(description = "面试会话 ID，不得猜测") String sessionId,
            @ToolParam(description = "复习任务 ID，不得猜测") String taskId,
            @ToolParam(description = "用户明确要求的计划复习日期，格式 yyyy-MM-dd")
            String plannedReviewDate,
            ToolContext toolContext
    ) {
        String assistantRequestId = markToolExecution(toolContext);
        String normalizedSessionId = requireText(sessionId, "面试会话 ID 不能为空");
        return executeLogged(SCHEDULE_REVIEW_TASK, assistantRequestId, normalizedSessionId, () -> {
            String normalizedTaskId = requireText(taskId, "复习任务 ID 不能为空");
            LocalDate normalizedDate = parseRequiredDate(plannedReviewDate);
            InterviewReviewTask task = reviewTaskService.schedule(
                    normalizedSessionId, normalizedTaskId, normalizedDate);
            return ReviewTaskScheduleToolResult.from(task);
        }, ignored -> 1);
    }

    private <T> T executeLogged(String toolName,
                                String assistantRequestId,
                                String sessionId,
                                Supplier<T> action,
                                ToIntFunction<T> resultCount) {
        String executionId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        try {
            T result = action.get();
            recordExecution(executionId, assistantRequestId, toolName, sessionId,
                    InterviewToolExecutionLog.Status.SUCCEEDED,
                    resultCount.applyAsInt(result), null, startedAt);
            return result;
        } catch (RuntimeException exception) {
            recordExecution(executionId, assistantRequestId, toolName, sessionId,
                    InterviewToolExecutionLog.Status.FAILED,
                    null, exception.getClass().getSimpleName(), startedAt);
            throw exception;
        }
    }

    private void recordExecution(String executionId,
                                 String assistantRequestId,
                                 String toolName,
                                 String sessionId,
                                 InterviewToolExecutionLog.Status status,
                                 Integer resultCount,
                                 String errorType,
                                 long startedAt) {
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        try {
            executionLogStore.record(new InterviewToolExecutionLog(
                    null,
                    executionId,
                    assistantRequestId,
                    toolName,
                    sessionId,
                    status,
                    resultCount,
                    elapsedMillis,
                    errorType,
                    LocalDateTime.now()));
        } catch (RuntimeException exception) {
            LOGGER.warn("记录面试工具执行日志失败，executionId={}，toolName={}",
                    executionId, toolName, exception);
        }
    }

    private String markToolExecution(ToolContext toolContext) {
        InterviewAssistantRequestContext requestContext =
                InterviewAssistantRequestContext.from(toolContext);
        return requestContext == null ? null : requestContext.markToolExecution();
    }

    private static LocalDate parseRequiredDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("计划复习日期不能为空");
        }
        return parseOptionalDate(value);
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

    public record ReviewTaskScheduleToolResult(
            String sessionId,
            String taskId,
            InterviewReviewTaskStatus status,
            LocalDate plannedReviewDate
    ) {

        static ReviewTaskScheduleToolResult from(InterviewReviewTask task) {
            return new ReviewTaskScheduleToolResult(
                    task.sessionId(), task.id(), task.status(), task.plannedReviewDate());
        }
    }
}
