package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.bank.BankGenerationService;
import com.javainterview.rag.interview.bank.BankGenerationTask;
import com.javainterview.rag.interview.bank.BankQuestion;
import com.javainterview.rag.interview.bank.QuestionBankService;
import com.javainterview.rag.interview.bank.QuestionBankStore;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.question.InterviewSkillTrainingProfile;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/question-bank")
@Tag(name = "知识题库", description = "资料出题、人工审核和专项面试")
public class QuestionBankController {
    private final QuestionBankService bank;
    private final BankGenerationService generation;
    private final QuestionBankStore store;

    public QuestionBankController(QuestionBankService bank, BankGenerationService generation,
                                    QuestionBankStore store) {
        this.bank = bank;
        this.generation = generation;
        this.store = store;
    }

    @GetMapping
    @Operation(summary = "筛选知识题库")
    public List<BankQuestion> list(@RequestParam String source,
                                   @RequestParam(required = false) BankQuestion.Status status,
                                   @RequestParam(required = false) InterviewDifficulty difficulty,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String keyword) {
        return bank.list(source, status, difficulty, category, keyword);
    }

    @PostMapping
    @Operation(summary = "新建人工题目草稿")
    public BankQuestion create(@RequestBody CreateQuestionRequest request) {
        return bank.create(request.source(), request.skill(), request.difficulty(), request.fields());
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑题目并撤销旧审核")
    public BankQuestion edit(@PathVariable String id, @RequestBody EditQuestionRequest request) {
        return bank.edit(id, expectedVersion(request.expectedVersion()), request.fields());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "审核启用或停用题目")
    public BankQuestion status(@PathVariable String id, @RequestBody StatusRequest request) {
        return bank.status(id, expectedVersion(request.expectedVersion()), request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除题目，保留历史会话快照")
    public void delete(@PathVariable String id, @RequestParam Long expectedVersion) {
        bank.delete(id, expectedVersion(expectedVersion));
    }

    @GetMapping("/tasks")
    @Operation(summary = "读取最近生成任务")
    public List<BankGenerationTask> tasks(@RequestParam String source) {
        return store.listTasks(BankQuestion.text(source, 500, "资料来源"));
    }

    @PostMapping("/tasks")
    @Operation(summary = "提交异步资料出题任务")
    public BankGenerationTask submit(@RequestBody GenerateRequest request) {
        return generation.submit(request.requestId(), request.source(), request.skill(), request.difficulty(),
                request.questionCount(), request.followUpCount());
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "查询生成进度，不调用模型")
    public BankGenerationTask task(@PathVariable String id) {
        return generation.find(id);
    }

    @PostMapping("/tasks/{id}/resume")
    @Operation(summary = "显式恢复失败题量，不重建成功题目")
    public BankGenerationTask resume(@PathVariable String id) {
        return generation.resume(id);
    }

    @PostMapping("/sessions")
    @Operation(summary = "从已审核题库创建专项面试")
    public InterviewSession start(@RequestBody QuestionBankService.SessionRequest request) {
        return bank.startSession(request);
    }

    @GetMapping("/training-plan")
    @Operation(summary = "读取 Skill 分类配额")
    public InterviewSkillTrainingProfile.TrainingPlan trainingPlan(
            @RequestParam InterviewSkill skill,
            @RequestParam int questionCount,
            @RequestParam(required = false) String focusDimension) {
        return bank.trainingPlan(skill, questionCount, focusDimension);
    }

    private long expectedVersion(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("必须提交有效的题目版本");
        }
        return value;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(InterviewSessionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(InterviewSessionConflictException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicate() {
        return Map.of("message", "当前资料版本已经存在相同题目");
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> unavailable() {
        return Map.of("message", "知识题库暂不可用，请稍后重试");
    }

    public record GenerateRequest(String requestId, String source, InterviewSkill skill,
                                    InterviewDifficulty difficulty, int questionCount, int followUpCount) { }

    public record StatusRequest(Long expectedVersion, BankQuestion.Status status) { }

    public record CreateQuestionRequest(String source, InterviewSkill skill, InterviewDifficulty difficulty,
                                         String category, String question, String topicSummary,
                                         String referenceAnswer, List<String> keyPoints, String scoringRubric,
                                         List<BankQuestion.FollowUp> followUps) {
        QuestionBankService.EditFields fields() {
            return new QuestionBankService.EditFields(category, question, topicSummary, referenceAnswer,
                    keyPoints, scoringRubric, followUps);
        }
    }

    public record EditQuestionRequest(Long expectedVersion, String category, String question, String topicSummary,
                                       String referenceAnswer, List<String> keyPoints, String scoringRubric,
                                       List<BankQuestion.FollowUp> followUps) {
        QuestionBankService.EditFields fields() {
            return new QuestionBankService.EditFields(category, question, topicSummary, referenceAnswer,
                    keyPoints, scoringRubric, followUps);
        }
    }
}
