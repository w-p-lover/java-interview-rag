package com.javainterview.rag.file;

public enum FilePurpose {
    KNOWLEDGE("knowledge"),
    RESUME("resume");

    private final String objectPrefix;

    FilePurpose(String objectPrefix) {
        this.objectPrefix = objectPrefix;
    }

    public String objectPrefix() {
        return objectPrefix;
    }
}
