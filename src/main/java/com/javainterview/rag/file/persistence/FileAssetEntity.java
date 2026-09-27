package com.javainterview.rag.file.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAssetEntity {
    private String id;
    private String purpose;
    private String objectKey;
    private String contentHash;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String status;
    private String errorType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
