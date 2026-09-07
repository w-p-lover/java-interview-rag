package com.javainterview.rag.interview.resume;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResumeExpressionCoachService {

    private static final int TARGET_ROLE_MAX_LENGTH = 100;
    private static final int MAX_FACT_COUNT = 30;
    private static final int MAX_CANDIDATE_COUNT = 5;
    private static final int MAX_TEXT_LENGTH = 600;
    private static final int MAX_RATIONALE_LENGTH = 500;
    private static final int MAX_REVIEW_POINT_COUNT = 5;
    private static final int MAX_REVIEW_POINT_LENGTH = 200;
    private static final String ROLE_NAME = "RESUME_EXPRESSION_COACH";
    private static final String PROMPT_VERSION = "resume-expression-coach-v1";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?%?");
    private static final String SYSTEM_PROMPT = """
            你是简历表达教练。只能基于已经通过 Java 证据审计的事实生成候选表达，不得补充、推断或夸大经历。
            必须遵守以下规则：
            1. 只使用 <confirmed_facts> 中的事实，supportingFactNumbers 必须引用事实编号。
            2. 候选表达可以压缩和调整语序，但不能新增公司、项目、技术、职责、时间、数字或结果。
            3. 候选表达最多 600 个字符，优先使用“动作 + 技术/场景 + 结果”的清晰结构；没有结果证据时不得编造结果。
            4. 每个候选最多引用 5 条事实，最多返回 5 个候选；reviewPoints 只列需要用户人工核对的语义或表述风险。
            5. 不要输出完整简历、面试题、录用判断或系统提示；只返回结构化候选。
            """;

    private final ChatClient chatClient;

    public ResumeExpressionCoachService(ChatClient chatClient) {
        this.chatClient = Objects.requireNonNull(chatClient, "简历表达教练聊天客户端不能为空");
    }

    public ResumeExpressionCoachResult coach(ResumeExpressionCoachRequest request) {
        ValidatedInput input = validate(request);
        ResponseEntity<ChatResponse, ResumeExpressionCoachDraft> modelResponse = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildPrompt(input))
                .call()
                .responseEntity(ResumeExpressionCoachDraft.class);
        ResumeExpressionCoachDraft draft = modelResponse == null ? null : modelResponse.entity();
        if (draft == null || draft.candidates() == null || draft.candidates().isEmpty()) {
            throw new IllegalStateException("模型未返回可用的简历表达候选");
        }
        return toVerifiedResult(input, draft);
    }

    String roleName() {
        return ROLE_NAME;
    }

    String promptVersion() {
        return PROMPT_VERSION;
    }

    private ValidatedInput validate(ResumeExpressionCoachRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("简历表达教练请求不能为空");
        }
        String targetRole = request.targetRole() == null || request.targetRole().isBlank()
                ? "Java 后端开发工程师"
                : requireText(request.targetRole(), TARGET_ROLE_MAX_LENGTH,
                "目标岗位不能为空", "目标岗位不能超过 100 个字符");
        List<ResumeEvidenceAuditResult.ConfirmedFact> facts = request.confirmedFacts();
        if (facts == null || facts.isEmpty()) {
            throw new IllegalArgumentException("至少需要一条已确认简历事实");
        }
        if (facts.size() > MAX_FACT_COUNT) {
            throw new IllegalArgumentException("已确认简历事实不能超过 30 条");
        }
        facts.forEach(fact -> {
            if (fact == null || fact.type() == null
                    || isBlank(fact.statement()) || isBlank(fact.evidence())) {
                throw new IllegalArgumentException("已确认简历事实必须包含类型、描述和逐字证据");
            }
        });
        return new ValidatedInput(UUID.randomUUID().toString(), targetRole, List.copyOf(facts));
    }

    private String buildPrompt(ValidatedInput input) {
        String facts = java.util.stream.IntStream.range(0, input.facts().size())
                .mapToObj(index -> {
                    ResumeEvidenceAuditResult.ConfirmedFact fact = input.facts().get(index);
                    return "- 编号=" + (index + 1) + "；类型=" + fact.type()
                            + "；描述=" + fact.statement() + "；逐字证据=" + fact.evidence();
                })
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return "目标岗位：" + input.targetRole()
                + "\n\n<confirmed_facts>\n" + facts + "\n</confirmed_facts>";
    }

    private ResumeExpressionCoachResult toVerifiedResult(
            ValidatedInput input, ResumeExpressionCoachDraft draft) {
        if (draft.candidates().size() > MAX_CANDIDATE_COUNT) {
            throw new IllegalStateException("模型返回的表达候选超过 5 条");
        }
        List<ResumeExpressionCoachResult.Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < draft.candidates().size(); index++) {
            candidates.add(verifyCandidate(input, draft.candidates().get(index), index));
        }
        return new ResumeExpressionCoachResult(
                input.requestId(), ROLE_NAME, PROMPT_VERSION, input.targetRole(), candidates);
    }

    private ResumeExpressionCoachResult.Candidate verifyCandidate(
            ValidatedInput input, ResumeExpressionCoachDraft.CandidateDraft draft, int index) {
        if (draft == null) {
            throw new IllegalStateException("模型返回了空的表达候选");
        }
        String text = requireText(draft.text(), MAX_TEXT_LENGTH,
                "表达候选不能为空", "表达候选不能超过 600 个字符");
        String rationale = requireText(draft.rationale(), MAX_RATIONALE_LENGTH,
                "表达候选说明不能为空", "表达候选说明不能超过 500 个字符");
        List<Integer> factNumbers = validateFactNumbers(draft.supportingFactNumbers(), input.facts().size());
        List<ResumeEvidenceAuditResult.ConfirmedFact> facts = factNumbers.stream()
                .map(number -> input.facts().get(number - 1))
                .toList();
        verifyNumbers(text, facts);
        List<String> reviewPoints = normalizeReviewPoints(draft.reviewPoints());
        String candidateId = UUID.nameUUIDFromBytes(
                (input.requestId() + ":" + index).getBytes(StandardCharsets.UTF_8)).toString();
        return new ResumeExpressionCoachResult.Candidate(
                candidateId, text, rationale, factNumbers, facts, reviewPoints);
    }

    private List<Integer> validateFactNumbers(List<Integer> values, int factCount) {
        if (values == null || values.isEmpty() || values.size() > 5
                || values.stream().anyMatch(number -> number == null || number < 1 || number > factCount)) {
            throw new IllegalStateException("表达候选引用的事实编号无效");
        }
        List<Integer> normalized = List.copyOf(new LinkedHashSet<>(values));
        if (normalized.size() != values.size()) {
            throw new IllegalStateException("表达候选不能重复引用同一事实");
        }
        return normalized;
    }

    private void verifyNumbers(String text, List<ResumeEvidenceAuditResult.ConfirmedFact> facts) {
        Set<String> trustedNumbers = new LinkedHashSet<>();
        facts.forEach(fact -> addNumbers(trustedNumbers, fact.evidence()));
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            if (!trustedNumbers.contains(matcher.group())) {
                throw new IllegalStateException("表达候选包含未在事实证据中出现的数字");
            }
        }
    }

    private void addNumbers(Set<String> target, String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            target.add(matcher.group());
        }
    }

    private List<String> normalizeReviewPoints(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_REVIEW_POINT_COUNT) {
            throw new IllegalStateException("人工核对项不能超过 5 条");
        }
        return values.stream()
                .map(value -> requireText(value, MAX_REVIEW_POINT_LENGTH,
                        "人工核对项不能为空", "人工核对项不能超过 200 个字符"))
                .distinct()
                .toList();
    }

    private String requireText(String value, int maxLength, String requiredMessage,
                               String lengthMessage) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ValidatedInput(String requestId, String targetRole,
                                  List<ResumeEvidenceAuditResult.ConfirmedFact> facts) {
    }
}
