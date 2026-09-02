package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class MarkdownSparseSearchConfiguration {

    @Bean
    @Profile("!storage")
    Directory localSparseIndexDirectory() {
        return new ByteBuffersDirectory();
    }

    @Bean
    @Profile("storage")
    Directory storageSparseIndexDirectory(
            @Value("${java-interview-rag.sparse.index-path:.tmp/sparse-index}") String path) {
        try {
            return FSDirectory.open(Path.of(path));
        } catch (IOException exception) {
            throw new IllegalStateException("初始化 BM25 文件索引目录失败：" + path, exception);
        }
    }

    @Bean(destroyMethod = "close")
    LuceneMarkdownChunkIndex luceneMarkdownChunkIndex(Directory directory) {
        return new LuceneMarkdownChunkIndex(directory);
    }

    @Bean
    MarkdownSparseSearchService markdownSparseSearchService(LuceneMarkdownChunkIndex index) {
        return new MarkdownSparseSearchService(index);
    }
}
