package com.javainterview.rag.interview.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeRecordEntity {
    private String id;
    private String name;
    private String targetRole;
    private String currentVersionId;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
