package com.javainterview.rag.modelprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderEntity {
    private String id;
    private String name;
    private String baseUrl;
    private String apiKeyNonce;
    private String apiKeyCiphertext;
    private String chatModel;
    private String embeddingModel;
    private Integer embeddingDimensions;
    private Double temperature;
    private boolean enabled;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
