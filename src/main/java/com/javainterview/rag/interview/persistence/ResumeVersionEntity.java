package com.javainterview.rag.interview.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersionEntity {
    private String id;
    private String resumeId;
    private int versionNumber;
    private String contentHash;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private String fileId;
    private String fullText;
    private LocalDateTime createdAt;
}
