package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionAnswer;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public Optional<InterviewSession> findById(String sessionId) {
        return Optional.ofNullable(mapper.selectById(sessionId)).map(this::toSession);
    }

    @Override
    public boolean update(InterviewSession session, long expectedVersion) {
        return mapper.updateState(toEntity(session), expectedVersion) == 1;
    }

    private InterviewSessionEntity toEntity(InterviewSession session) {
        try {
            return InterviewSessionEntity.from(
                    session,
                    objectMapper.writeValueAsString(session.profile()),
                    objectMapper.writeValueAsString(session.questions()),
                    objectMapper.writeValueAsString(session.answers())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化面试会话失败", exception);
        }
    }

    private InterviewSession toSession(InterviewSessionEntity entity) {
        try {
            return new InterviewSession(
                    entity.getId(),
                    entity.getTargetRole(),
                    objectMapper.readValue(entity.getProfileJson(), InterviewPreparationProfile.class),
                    objectMapper.readValue(entity.getQuestionsJson(), QUESTION_LIST_TYPE),
                    objectMapper.readValue(entity.getAnswersJson(), ANSWER_LIST_TYPE),
                    entity.getCurrentQuestionIndex(),
                    InterviewSessionStatus.valueOf(entity.getStatus()),
                    entity.getVersion(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("面试会话数据损坏", exception);
        }
    }
}
