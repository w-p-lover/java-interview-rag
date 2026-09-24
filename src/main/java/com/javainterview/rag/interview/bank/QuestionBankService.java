package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewQuestionAllocation;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.question.InterviewSkillTrainingCatalog;
import com.javainterview.rag.interview.question.InterviewSkillTrainingProfile;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionConflictException;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.function.Supplier;

public class QuestionBankService {
    private final QuestionBankStore store;
    private final BankSources sources;
    private final InterviewSessionService sessionService;
    private final InterviewSessionStore sessionStore;
    private final Supplier<String> modelNameSupplier;
    private final InterviewSkillTrainingCatalog trainingCatalog = new InterviewSkillTrainingCatalog();

    public QuestionBankService(QuestionBankStore store, BankSources sources,
                               InterviewSessionService sessionService,
                               InterviewSessionStore sessionStore, String modelName) {
        this(store, sources, sessionService, sessionStore, () -> modelName);
    }

    public QuestionBankService(QuestionBankStore store, BankSources sources,
                               InterviewSessionService sessionService,
                               InterviewSessionStore sessionStore, Supplier<String> modelNameSupplier) {
        this.store = store;
        this.sources = sources;
        this.sessionService = sessionService;
        this.sessionStore = sessionStore;
        this.modelNameSupplier = Objects.requireNonNull(modelNameSupplier, "模型契约提供器不能为空");
    }

    public List<BankQuestion> list(String source, BankQuestion.Status status,
                                    InterviewDifficulty difficulty, String category, String keyword) {
        source = BankQuestion.text(source, 500, "资料来源");
        String currentVersion = sources.currentVersion(source);
        String normalizedKeyword = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return store.listQuestions(source).stream()
                .map(question -> refreshState(question, currentVersion))
                .filter(question -> status == null || question.status() == status)
                .filter(question -> difficulty == null || question.difficulty() == difficulty)
                .filter(question -> category == null || category.isBlank() || question.category().equals(category.strip()))
                .filter(question -> normalizedKeyword.isEmpty()
                        || String.join("\n", question.question(), question.referenceAnswer(),
                        question.topicSummary(), question.category(), question.scoringRubric())
                        .toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .toList();
    }

    private BankQuestion refreshState(BankQuestion question, String currentVersion) {
        if (!question.documentVersion().equals(currentVersion)
                && question.status() != BankQuestion.Status.ARCHIVED && question.status() != BankQuestion.Status.STALE) {
            BankQuestion stale = question.withState(BankQuestion.Status.STALE,
                    question.version() + 1, LocalDateTime.now());
            if (store.updateQuestion(stale, question.version())) {
                return stale;
            }
            return requireQuestion(question.id());
        }
        return question;
    }

    public BankQuestion create(String source, InterviewSkill skill,
                                InterviewDifficulty difficulty, EditFields fields) {
        source = BankQuestion.text(source, 500, "资料来源");
        if (skill == null || skill == InterviewSkill.GENERAL || difficulty == null) {
            throw new IllegalArgumentException("请选择具体 Skill 和难度");
        }
        String version = sources.currentSnapshot(source).document().version();
        String evidenceQuery = String.join("\n", fields.question(), fields.topicSummary(),
                fields.referenceAnswer(), String.join("\n", fields.keyPoints()),
                fields.followUps().stream()
                        .map(followUp -> String.join("\n", followUp.question(),
                                followUp.referenceAnswer(), String.join("\n", followUp.keyPoints())))
                        .collect(java.util.stream.Collectors.joining("\n")));
        var evidence = sources.relevantEvidence(source, version, evidenceQuery, 3);
        LocalDateTime now = LocalDateTime.now();
        BankQuestion question = new BankQuestion(UUID.randomUUID().toString(), null, source, version,
                skill, difficulty, fields.category(), fields.question(), fields.topicSummary(),
                fields.referenceAnswer(), fields.keyPoints(), fields.scoringRubric(), fields.followUps(),
                evidence, BankQuestion.Status.DRAFT, 0, 0, null, now, now);
        if (!store.insertQuestion(question)) {
            throw new InterviewSessionConflictException("当前资料版本已经存在相同题目");
        }
        return question;
    }

    public BankQuestion edit(String id, long expectedVersion, EditFields fields) {
        BankQuestion original = requireVersion(id, expectedVersion);
        requireCurrent(original);
        BankQuestion updated = new BankQuestion(original.id(), original.generationTaskId(),
                original.source(), original.documentVersion(), original.skill(), original.difficulty(),
                fields.category(), fields.question(), fields.topicSummary(), fields.referenceAnswer(),
                fields.keyPoints(), fields.scoringRubric(), fields.followUps(), original.evidence(),
                BankQuestion.Status.DRAFT, original.version() + 1, original.useCount(),
                original.lastUsedAt(), original.createdAt(), LocalDateTime.now());
        if (!store.updateQuestion(updated, expectedVersion)) {
            throw conflict();
        }
        return updated;
    }

    public BankQuestion status(String id, long expectedVersion, BankQuestion.Status status) {
        if (status == null || status == BankQuestion.Status.STALE) {
            throw new IllegalArgumentException("请选择草稿、启用或停用状态");
        }
        BankQuestion original = requireVersion(id, expectedVersion);
        if (status != BankQuestion.Status.ARCHIVED) {
            requireCurrent(original);
        }
        BankQuestion updated = original.withState(status, original.version() + 1, LocalDateTime.now());
        if (!store.updateQuestion(updated, expectedVersion)) {
            throw conflict();
        }
        return updated;
    }

    public void delete(String id, long expectedVersion) {
        requireVersion(id, expectedVersion);
        if (!store.deleteQuestion(id, expectedVersion)) {
            throw conflict();
        }
    }

    private BankQuestion requireQuestion(String id) {
        return store.findQuestion(BankQuestion.text(id, 36, "题目 ID"))
                .orElseThrow(() -> new NoSuchElementException("题库题目不存在"));
    }

    private BankQuestion requireVersion(String id, long expectedVersion) {
        BankQuestion question = requireQuestion(id);
        if (question.version() != expectedVersion) {
            throw conflict();
        }
        return question;
    }

    private void requireCurrent(BankQuestion question) {
        if (!sources.isCurrent(question.source(), question.documentVersion())) {
            refreshState(question, sources.currentVersion(question.source()));
            throw new InterviewSessionConflictException("题目绑定的资料已更新或删除，请从当前版本重新生成并审核");
        }
    }

    private InterviewSessionConflictException conflict() {
        return new InterviewSessionConflictException("题目已被更新，请刷新后重试；当前编辑内容仍可保留");
    }

    public InterviewSkillTrainingProfile.TrainingPlan trainingPlan(
            InterviewSkill skill,
            int questionCount,
            String focusDimension) {
        return trainingCatalog.plan(skill, questionCount, focusDimension);
    }

    @Transactional
    public InterviewSession startSession(SessionRequest request) {
        String requestId = "bank:" + BankQuestion.text(request.requestId(), 80, "创建请求 ID");
        String source = BankQuestion.text(request.source(), 500, "资料来源");
        var existing = sessionStore.findByRequestId(requestId);
        if (existing.isPresent()) {
            recordUsage(existing.get());
            return existing.get();
        }
        if (request.questionCount() < 3 || request.questionCount() > 8
                || request.followUpCount() < 0 || request.followUpCount() > 2) {
            throw new IllegalArgumentException("主问题数必须在 3 到 8 之间，固定追问数在 0 到 2 之间");
        }
        if (request.skill() == null || request.skill() == InterviewSkill.GENERAL || request.difficulty() == null) {
            throw new IllegalArgumentException("请选择具体 Skill 和难度");
        }
        boolean hasCategory = request.category() != null && !request.category().isBlank();
        boolean hasFocus = request.focusDimension() != null && !request.focusDimension().isBlank();
        if (hasCategory && hasFocus) {
            throw new IllegalArgumentException("显式分类筛选与薄弱维度训练不能同时使用");
        }
        if (request.questionIds() != null && !request.questionIds().isEmpty() && hasFocus) {
            throw new IllegalArgumentException("人工选题不支持薄弱维度自动配额");
        }
        int total = request.questionCount() * (request.followUpCount() + 1);
        if (total < 3 || total > 8) {
            throw new IllegalArgumentException("主问题与固定追问合计必须在 3 到 8 题之间");
        }
        sources.currentSnapshot(source);
        List<BankQuestion> selected;
        if (request.questionIds() != null && !request.questionIds().isEmpty()) {
            if (request.questionIds().size() != request.questionCount()
                    || request.questionIds().stream().distinct().count() != request.questionCount()) {
                throw new IllegalArgumentException("人工选题数量必须与主问题数一致且不得重复");
            }
            selected = request.questionIds().stream().map(this::requireQuestion).toList();
        } else {
            List<BankQuestion> candidates = new ArrayList<>(list(source, BankQuestion.Status.ACTIVE,
                    request.difficulty(), hasCategory ? request.category() : null, null).stream()
                    .filter(question -> question.skill() == request.skill()
                            && question.followUps().size() >= request.followUpCount()).toList());
            if (hasCategory) {
                if (candidates.size() < request.questionCount()) {
                    throw insufficientTotal(request, candidates.size());
                }
                Collections.shuffle(candidates);
                selected = candidates.subList(0, request.questionCount());
            } else {
                selected = selectByTrainingPlan(candidates, trainingPlan(
                        request.skill(), request.questionCount(), request.focusDimension()));
            }
        }
        List<InterviewQuestion> questions = new ArrayList<>();
        for (BankQuestion question : selected) {
            requireCurrent(question);
            if (!source.equals(question.source()) || question.status() != BankQuestion.Status.ACTIVE
                    || question.skill() != request.skill() || question.difficulty() != request.difficulty()
                    || (hasCategory
                    && !request.category().strip().equals(question.category()))) {
                throw new InterviewSessionConflictException("选题必须来自同一资料、Skill、难度和分类的已审核题目");
            }
            if (question.followUps().size() < request.followUpCount()) {
                throw new InterviewSessionConflictException("题目固定追问数量不足，请减少追问或补充题库");
            }
            int parentIndex = questions.size();
            questions.add(toInterviewQuestion(question, question.question(), question.referenceAnswer(),
                    question.keyPoints(), question.scoringRubric(), false, null));
            List<BankQuestion.FollowUp> followUps = new ArrayList<>(question.followUps());
            Collections.shuffle(followUps);
            for (BankQuestion.FollowUp followUp : followUps.subList(0, request.followUpCount())) {
                questions.add(toInterviewQuestion(question, followUp.question(), followUp.referenceAnswer(),
                        followUp.keyPoints(), followUp.scoringRubric(), true, parentIndex));
            }
        }
        if (questions.stream().map(question -> BankQuestionKeys.normalize(question.question()))
                .distinct().count() != questions.size()) {
            throw new InterviewSessionConflictException("所选主问题或固定追问存在重复，请编辑题库后重试");
        }
        InterviewPreparationProfile profile = new InterviewPreparationProfile(
                hasFocus ? "知识资料薄弱维度专项训练，不包含简历或岗位事实"
                        : "知识资料专项训练，不包含简历或岗位事实",
                List.of("本场只考察已审核知识题库，不推断候选人项目经历"), List.of(),
                List.of(new InterviewFocusArea(request.skill().name(), hasFocus
                        ? "基于当前资料强化“" + request.focusDimension().strip() + "”"
                        : "基于当前资料的已审核题目",
                        List.of(InterviewFocusSource.TARGET_ROLE))));
        var allocations = IntStream.range(0, questions.size())
                .mapToObj(index -> new InterviewQuestionAllocation(index,
                        InterviewQuestionSource.KNOWLEDGE, List.of())).toList();
        InterviewConfigurationSnapshot configuration = new InterviewConfigurationSnapshot(
                "knowledge-question-bank-v3", InterviewSessionMode.STANDARD, request.skill(),
                request.difficulty(), questions.size(), questions.size(),
                Map.of(InterviewQuestionSource.KNOWLEDGE, questions.size()), allocations,
                "knowledge-bank-profile-v1", BankQuestionGenerator.PROMPT_VERSION, currentModelName());
        InterviewSession session = sessionService.start("知识资料专项面试", profile, questions,
                questions.size(), configuration, requestId);
        recordUsage(session);
        return session;
    }

    private String currentModelName() {
        String value = modelNameSupplier.get();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("知识题库模型契约不能为空");
        }
        return value.strip();
    }

    private void recordUsage(InterviewSession session) {
        for (int index = 0; index < session.questions().size(); index++) {
            QuestionBankContext context = session.questions().get(index).bankContext();
            if (context != null && !context.fixedFollowUp()) {
                store.recordUsage(session.id(), context.questionId(), index);
            }
        }
    }

    private List<BankQuestion> selectByTrainingPlan(
            List<BankQuestion> candidates,
            InterviewSkillTrainingProfile.TrainingPlan plan) {
        Map<String, List<BankQuestion>> byCategory = candidates.stream()
                .collect(java.util.stream.Collectors.groupingBy(BankQuestion::category));
        List<BankQuestion> selected = new ArrayList<>();
        for (InterviewSkillTrainingProfile.CategoryQuota quota : plan.categories()) {
            List<BankQuestion> categoryQuestions = new ArrayList<>(
                    byCategory.getOrDefault(quota.category(), List.of()));
            if (categoryQuestions.size() < quota.questionCount()) {
                throw new InterviewSessionConflictException("分类“" + quota.category()
                        + "”可用题目不足：需要 " + quota.questionCount()
                        + " 道，目前只有 " + categoryQuestions.size() + " 道");
            }
            Collections.shuffle(categoryQuestions);
            selected.addAll(categoryQuestions.subList(0, quota.questionCount()));
        }
        return List.copyOf(selected);
    }

    private InterviewSessionConflictException insufficientTotal(SessionRequest request, int actualCount) {
        return new InterviewSessionConflictException("可用题目不足：需要 " + request.questionCount()
                + " 道当前版本已审核题，且每题至少 " + request.followUpCount()
                + " 个固定追问；目前只有 " + actualCount + " 道");
    }

    private InterviewQuestion toInterviewQuestion(BankQuestion source, String question,
                                                   String answer, List<String> keyPoints,
                                                   String rubric, boolean followUp, Integer parentIndex) {
        String objective = followUp
                ? source.topicSummary() + " · 固定追问维度：" + source.category()
                : source.topicSummary();
        return new InterviewQuestion(question, objective,
                List.of(InterviewFocusSource.TARGET_ROLE), question, true, source.evidence(),
                new QuestionBankContext(source.id(), answer, keyPoints, rubric,
                        followUp, parentIndex, source.category()));
    }

    public record EditFields(String category, String question, String topicSummary,
                              String referenceAnswer, List<String> keyPoints,
                              String scoringRubric, List<BankQuestion.FollowUp> followUps) { }

    public record SessionRequest(String requestId, String source, List<String> questionIds,
                                   int questionCount, int followUpCount, InterviewSkill skill,
                                   InterviewDifficulty difficulty, String category,
                                   String focusDimension) { }
}
