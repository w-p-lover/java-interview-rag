package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.resume.ResumeCenterConflictException;
import com.javainterview.rag.interview.resume.ResumeCenterStore;
import com.javainterview.rag.interview.resume.ResumeRecord;
import com.javainterview.rag.interview.resume.ResumeVersion;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MySqlResumeCenterStore implements ResumeCenterStore {

    private final ResumeCenterMapper mapper;
    private final TransactionTemplate transactions;

    public MySqlResumeCenterStore(ResumeCenterMapper mapper, TransactionTemplate transactions) {
        this.mapper = Objects.requireNonNull(mapper, "简历中心 Mapper 不能为空");
        this.transactions = Objects.requireNonNull(transactions, "事务模板不能为空");
    }

    @Override
    public List<ResumeRecord> findAll() {
        return mapper.findAllRecords().stream().map(this::record).toList();
    }

    @Override
    public Optional<ResumeRecord> findRecord(String id) {
        return Optional.ofNullable(mapper.findRecord(id)).map(this::record);
    }

    @Override
    public Optional<ResumeVersion> findVersion(String id) {
        return Optional.ofNullable(mapper.findVersion(id)).map(this::version);
    }

    @Override
    public List<ResumeVersion> findVersions(String resumeId) {
        return mapper.findVersions(resumeId).stream().map(this::version).toList();
    }

    @Override
    public Optional<ResumeVersion> findVersionByHash(String contentHash) {
        return Optional.ofNullable(mapper.findVersionByHash(contentHash)).map(this::version);
    }

    @Override
    public SaveVersionResult saveVersion(SaveVersionRequest request) {
        ResumeVersion existing = findVersionByHash(request.contentHash()).orElse(null);
        if (existing != null) {
            return duplicate(existing);
        }
        try {
            return Objects.requireNonNull(transactions.execute(status -> saveInTransaction(request)));
        } catch (DuplicateKeyException exception) {
            ResumeVersion winner = findVersionByHash(request.contentHash())
                    .orElseThrow(() -> exception);
            return duplicate(winner);
        }
    }

    private SaveVersionResult saveInTransaction(SaveVersionRequest request) {
        ResumeVersionEntity duplicate = mapper.findVersionByHash(request.contentHash());
        if (duplicate != null) {
            return duplicate(version(duplicate));
        }
        String versionId = UUID.randomUUID().toString();
        if (request.resumeId() == null) {
            String resumeId = UUID.randomUUID().toString();
            ResumeRecordEntity record = new ResumeRecordEntity(
                    resumeId, request.name(), request.targetRole(), versionId,
                    0, request.createdAt(), request.createdAt());
            ResumeVersionEntity version = entity(request, resumeId, versionId, 1);
            mapper.insertRecord(record);
            mapper.insertVersion(version);
            return new SaveVersionResult(record(record), version(version), false);
        }

        ResumeRecordEntity current = mapper.lockRecord(request.resumeId());
        if (current == null) {
            throw new NoSuchElementException("简历记录不存在");
        }
        int nextVersionNumber = mapper.findLatestVersionNumber(current.getId()) + 1;
        ResumeVersionEntity version = entity(
                request, current.getId(), versionId, nextVersionNumber);
        mapper.insertVersion(version);
        if (mapper.updateCurrentVersion(
                current.getId(), request.name(), request.targetRole(), versionId,
                current.getVersion(), request.createdAt()) != 1) {
            throw new ResumeCenterConflictException("简历记录已更新，请刷新后重试");
        }
        ResumeRecord updated = new ResumeRecord(
                current.getId(), request.name(), request.targetRole(), versionId,
                current.getVersion() + 1, current.getCreatedAt(), request.createdAt());
        return new SaveVersionResult(updated, version(version), false);
    }

    @Override
    public boolean switchCurrent(String resumeId, String versionId,
                                 long expectedVersion, LocalDateTime updatedAt) {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            ResumeRecordEntity current = mapper.lockRecord(resumeId);
            ResumeVersionEntity version = mapper.findVersion(versionId);
            if (current == null || current.getVersion() != expectedVersion
                    || version == null || !resumeId.equals(version.getResumeId())) {
                return false;
            }
            return mapper.updateCurrentVersion(
                    current.getId(), current.getName(), current.getTargetRole(), versionId,
                    expectedVersion, updatedAt) == 1;
        }));
    }

    @Override
    public boolean enrichVersionStorage(String versionId, String fileId, String fullText) {
        return mapper.enrichVersionStorage(versionId, fileId, fullText) == 1;
    }

    private SaveVersionResult duplicate(ResumeVersion version) {
        ResumeRecord record = findRecord(version.resumeId())
                .orElseThrow(() -> new IllegalStateException("重复简历版本缺少所属记录"));
        return new SaveVersionResult(record, version, true);
    }

    private ResumeVersionEntity entity(SaveVersionRequest request, String resumeId,
                                       String versionId, int versionNumber) {
        return new ResumeVersionEntity(
                versionId, resumeId, versionNumber, request.contentHash(),
                request.originalFilename(), request.contentType(), request.fileSize(),
                request.fileId(), request.fullText(), request.createdAt());
    }

    private ResumeRecord record(ResumeRecordEntity entity) {
        return new ResumeRecord(
                entity.getId(), entity.getName(), entity.getTargetRole(),
                entity.getCurrentVersionId(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private ResumeVersion version(ResumeVersionEntity entity) {
        return new ResumeVersion(
                entity.getId(), entity.getResumeId(), entity.getVersionNumber(),
                entity.getContentHash(), entity.getOriginalFilename(), entity.getContentType(),
                entity.getFileSize(), entity.getFileId(), entity.getFullText(), entity.getCreatedAt());
    }
}
