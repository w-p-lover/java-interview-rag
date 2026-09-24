package com.javainterview.rag.modelprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderDefaultsEntity {
    private int id;
    private String chatProviderId;
    private String embeddingProviderId;
    private long version;
    private LocalDateTime updatedAt;
}
