package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarkdownImportConfiguration {

    private static final int MAX_CHUNK_LENGTH = 800;

    @Bean
    MarkdownImportRegistry markdownImportRegistry() {
        return new MarkdownImportRegistry();
    }

    @Bean
    MarkdownContentImporter markdownContentImporter(MarkdownImportRegistry registry) {
        return new MarkdownContentImporter(registry, MAX_CHUNK_LENGTH);
    }
}
