package com.javainterview.rag.file;

public class FileAssetUnavailableException extends RuntimeException {
    public FileAssetUnavailableException(String message) {
        super(message);
    }

    public FileAssetUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
