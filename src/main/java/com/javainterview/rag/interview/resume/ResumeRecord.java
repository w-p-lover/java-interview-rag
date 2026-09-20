package com.javainterview.rag.interview.resume;

import java.time.LocalDateTime;

public record ResumeRecord(
        String id,
        String name,
        String targetRole,
        String currentVersionId,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ResumeRecord {
        id = text(id, 36, "简历记录 ID");
        name = text(name, 200, "简历名称");
        targetRole = text(targetRole, 200, "目标岗位");
        if (currentVersionId != null) {
            currentVersionId = text(currentVersionId, 36, "当前简历版本 ID");
        }
        if (version < 0) {
            throw new IllegalArgumentException("简历记录版本不能小于 0");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("简历记录时间不能为空");
        }
    }

    static String text(String value, int limit, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.length() > limit) {
            throw new IllegalArgumentException(name + "不能超过 " + limit + " 个字符");
        }
        return normalized;
    }
}
