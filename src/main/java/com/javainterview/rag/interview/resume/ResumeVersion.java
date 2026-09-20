package com.javainterview.rag.interview.resume;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ResumeVersion(
        String id,
        String resumeId,
        int versionNumber,
        String contentHash,
        String originalFilename,
        String contentType,
        long fileSize,
        @JsonIgnore String fullText,
        LocalDateTime createdAt
) {
    public ResumeVersion {
        id = ResumeRecord.text(id, 36, "简历版本 ID");
        resumeId = ResumeRecord.text(resumeId, 36, "简历记录 ID");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("简历版本号必须大于 0");
        }
        contentHash = ResumeRecord.text(contentHash, 64, "简历内容 Hash");
        if (!contentHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("简历内容 Hash 格式无效");
        }
        originalFilename = ResumeRecord.text(originalFilename, 255, "简历文件名");
        contentType = ResumeRecord.text(contentType, 100, "简历文件类型");
        if (fileSize < 1 || fileSize > ResumeCenterService.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("简历文件大小无效");
        }
        if (fullText != null && (fullText.isBlank() || fullText.length() > ResumeCenterService.MAX_TEXT_LENGTH)) {
            throw new IllegalArgumentException("保存的简历正文为空或过长");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("简历版本创建时间不能为空");
        }
    }

    @JsonProperty("fullTextSaved")
    public boolean fullTextSaved() {
        return fullText != null;
    }
}
