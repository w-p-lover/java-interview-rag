package com.javainterview.rag.modelprovider;

import java.time.Instant;

public record ModelProviderTestResult(
        ModelCapability capability,
        boolean success,
        String code,
        String message,
        String modelName,
        Integer actualDimensions,
        long durationMillis,
        Instant testedAt) {
}
