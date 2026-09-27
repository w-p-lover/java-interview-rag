package com.javainterview.rag.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileObjectStorageTest {

    @TempDir
    Path root;

    @Test
    void writesReadsAndDeletesWithinConfiguredRoot() {
        LocalFileObjectStorage storage = new LocalFileObjectStorage(root);
        byte[] content = "知识资料".getBytes(StandardCharsets.UTF_8);

        storage.put("knowledge/ab/hash", content, "text/plain");

        assertThat(storage.exists("knowledge/ab/hash")).isTrue();
        assertThat(storage.read("knowledge/ab/hash")).isEqualTo(content);
        storage.delete("knowledge/ab/hash");
        assertThat(storage.exists("knowledge/ab/hash")).isFalse();
    }

    @Test
    void rejectsObjectKeyThatEscapesStorageRoot() {
        LocalFileObjectStorage storage = new LocalFileObjectStorage(root);

        assertThatThrownBy(() -> storage.read("../outside"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能越过");
    }
}
