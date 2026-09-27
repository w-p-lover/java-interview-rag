package com.javainterview.rag.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileAssetServiceTest {

    @TempDir
    Path root;

    @Test
    void deduplicatesSamePurposeAndContentAndVerifiesDownload() {
        FileAssetService service = new FileAssetService(
                new InMemoryFileAssetStore(), new LocalFileObjectStorage(root));
        byte[] content = "# Java\n线程池".getBytes(StandardCharsets.UTF_8);

        FileAsset first = service.store(FilePurpose.KNOWLEDGE, "java.md", "text/markdown", content);
        FileAsset duplicate = service.store(FilePurpose.KNOWLEDGE, "copy.md", "text/markdown", content);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(service.read(first.id()).content()).isEqualTo(content);
        assertThat(service.inspect(first.id()).consistent()).isTrue();
    }

    @Test
    void refusesDeletionWhileBusinessReferenceExists() {
        ReferencedStore store = new ReferencedStore();
        FileAssetService service = new FileAssetService(store, new LocalFileObjectStorage(root));
        FileAsset asset = service.store(FilePurpose.RESUME, "resume.md", "text/markdown",
                "resume".getBytes(StandardCharsets.UTF_8));
        store.referencedId = asset.id();

        assertThatThrownBy(() -> service.deleteIfUnreferenced(asset.id()))
                .isInstanceOf(FileAssetConflictException.class)
                .hasMessageContaining("业务记录引用");
    }

    @Test
    void reportsReadyMetadataWithMissingObjectAsUnavailable() {
        InMemoryFileAssetStore store = new InMemoryFileAssetStore();
        LocalFileObjectStorage storage = new LocalFileObjectStorage(root);
        FileAssetService service = new FileAssetService(store, storage);
        FileAsset asset = service.store(FilePurpose.KNOWLEDGE, "missing.md", "text/plain",
                "missing".getBytes(StandardCharsets.UTF_8));
        storage.delete(asset.objectKey());

        assertThatThrownBy(() -> service.read(asset.id()))
                .isInstanceOf(FileAssetUnavailableException.class)
                .hasMessageContaining("对象内容缺失");
        assertThat(service.inspect(asset.id()).consistent()).isFalse();
    }

    private static final class ReferencedStore implements FileAssetStore {
        private final InMemoryFileAssetStore delegate = new InMemoryFileAssetStore();
        private String referencedId;

        @Override public java.util.Optional<FileAsset> find(String id) { return delegate.find(id); }
        @Override public java.util.Optional<FileAsset> findByPurposeAndHash(FilePurpose purpose, String hash) { return delegate.findByPurposeAndHash(purpose, hash); }
        @Override public FileAsset createUploading(FileAsset asset) { return delegate.createUploading(asset); }
        @Override public boolean markUploading(String id, String name, String type, long size, LocalDateTime time) { return delegate.markUploading(id, name, type, size, time); }
        @Override public boolean markReady(String id, LocalDateTime time) { return delegate.markReady(id, time); }
        @Override public boolean markFailed(String id, String error, LocalDateTime time) { return delegate.markFailed(id, error, time); }
        @Override public boolean markDeletePending(String id, LocalDateTime time) { return delegate.markDeletePending(id, time); }
        @Override public boolean markDeleted(String id, LocalDateTime time) { return delegate.markDeleted(id, time); }
        @Override public int countBusinessReferences(String id) { return id.equals(referencedId) ? 1 : 0; }
    }
}
