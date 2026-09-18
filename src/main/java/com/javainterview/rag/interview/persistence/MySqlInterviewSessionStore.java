package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.HistoricalInterviewQuestion;
import com.javainterview.rag.interview.question.InterviewQuestionAllocation;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSessionAnswer;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;

public class MySqlInterviewSessionStore implements InterviewSessionStore {

    private static final TypeReference<List<InterviewQuestion>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<InterviewSessionAnswer>> ANSWER_LIST_TYPE = new TypeReference<>() {
    };

    private final InterviewSessionMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlInterviewSessionStore(InterviewSessionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "面试会话 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public void create(InterviewSession session) {
        mapper.insert(toEntity(session));
    }

    @Override
    public Optional<InterviewSession> createIfAbsent(InterviewSession session, String requestId) {
        try {
            mapper.insert(toEntity(session, requestId));
            return Optional.empty();
        } catch (DuplicateKeyException exception) {
            return findByRequestId(requestId);
        }
    }

    @Override
    public Optional<InterviewSession> findById(String sessionId) {
        return Optional.ofNullable(mapper.selectById(sessionId)).map(this::toSession);
    }

    @Override
    public Optional<InterviewSession> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectByRequestId(requestId.strip())).map(this::toSession);
    }

    @Override
    public List<HistoricalInterviewQuestion> findRecentQuestions(InterviewSkill skill, int limit) {
        if (skill == null || limit < 1) {
            return List.of();
        }
        List<HistoricalInterviewQuestion> history = new ArrayList<>();
        List<InterviewSessionEntity> entities = mapper.selectList(new QueryWrapper<InterviewSessionEntity>()
                .eq("session_mode", "STANDARD")
                .orderByDesc("updated_at")
                .last("LIMIT " + Math.min(limit, 100)));
        for (InterviewSessionEntity entity : entities) {
            if (history.size() >= limit) {
                break;
            }
            try {
                InterviewConfigurationSnapshot configuration = entity.getConfigurationJson() == null
                        ? null : objectMapper.readValue(
                        entity.getConfigurationJson(), InterviewConfigurationSnapshot.class);
                if (configuration == null || configuration.skill() != skill) {
                    continue;
                }
                List<InterviewQuestion> questions = objectMapper.readValue(
                        entity.getQuestionsJson(), QUESTION_LIST_TYPE);
                for (InterviewQuestionAllocation allocation : configuration.questionAllocations()) {
                    if (allocation.questionIndex() >= questions.size()) {
                        continue;
                    }
                    InterviewQuestion question = questions.get(allocation.questionIndex());
                    history.add(new HistoricalInterviewQuestion(
                            question.question(),
                            allocation.primarySource().name(),
                            question.objective()));
                    if (history.size() >= limit) {
                        break;
                    }
                }
            } catch (JsonProcessingException | RuntimeException ignored) {
                // 历史题只用于降低重复率，旧数据损坏不阻断当前草稿生成。
            }
        }
        return List.copyOf(history);
    }

    @Override
    public boolean update(InterviewSession session, long expectedVersion) {
        return mapper.updateState(toEntity(session), expectedVersion) == 1;
    }

    private InterviewSessionEntity toEntity(InterviewSession session) {
        return toEntity(session, null);
    }

    private InterviewSessionEntity toEntity(InterviewSession session, String requestId) {
        try {
            return InterviewSessionEntity.from(
                    session, requestId,
                    objectMapper.writeValueAsString(session.configuration()),
                    objectMapper.writeValueAsString(session.profile()),
                    objectMapper.writeValueAsString(session.questions()),
                    objectMapper.writeValueAsString(session.answers()),
                    session.report() == null ? null : objectMapper.writeValueAsString(session.report())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化面试会话失败", exception);
        }
    }

    private InterviewSession toSession(InterviewSessionEntity entity) {
        try {
            List<InterviewQuestion> questions = objectMapper.readValue(
                    entity.getQuestionsJson(), QUESTION_LIST_TYPE);
            InterviewSessionMode mode = entity.getSessionMode() == null
                    ? InterviewSessionMode.STANDARD
                    : InterviewSessionMode.valueOf(entity.getSessionMode());
            InterviewConfigurationSnapshot configuration = entity.getConfigurationJson() == null
                    ? InterviewConfigurationSnapshot.legacy(
                    mode, questions.size(), entity.getMaxQuestionCount())
                    : objectMapper.readValue(
                    entity.getConfigurationJson(), InterviewConfigurationSnapshot.class);
            return new InterviewSession(
                    entity.getId(),
                    entity.getTargetRole(),
                    mode,
                    objectMapper.readValue(entity.getProfileJson(), InterviewPreparationProfile.class),
                    configuration,
                    questions,
                    entity.getMaxQuestionCount(),
                    objectMapper.readValue(entity.getAnswersJson(), ANSWER_LIST_TYPE),
                    entity.getCurrentQuestionIndex(),
                    InterviewSessionStatus.valueOf(entity.getStatus()),
                    entity.getVersion(),
                    entity.getReportJson() == null ? null
                            : objectMapper.readValue(entity.getReportJson(),
                            com.javainterview.rag.interview.session.InterviewSessionReport.class),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("面试会话数据损坏", exception);
        }
    }
}
