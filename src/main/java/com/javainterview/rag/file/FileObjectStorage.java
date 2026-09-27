package com.javainterview.rag.file;

public interface FileObjectStorage {

    void put(String objectKey, byte[] content, String contentType);

    byte[] read(String objectKey);

    boolean exists(String objectKey);

    void delete(String objectKey);
}
