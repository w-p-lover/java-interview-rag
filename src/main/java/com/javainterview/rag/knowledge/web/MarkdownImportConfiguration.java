package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.InMemoryMarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.persistence.KnowledgeDocumentVersionMapper;
import com.javainterview.rag.knowledge.persistence.MySqlMarkdownImportRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class MarkdownImportConfiguration {

    private static final int MAX_CHUNK_LENGTH = 800;

    @Bean
    @Profile("!storage")
    MarkdownImportRegistry markdownImportRegistry() {
        return new InMemoryMarkdownImportRegistry();
    }

    @Bean
    @Profile("storage")
    MarkdownImportRegistry mysqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper mapper) {
        return new MySqlMarkdownImportRegistry(mapper);
    }

    @Bean
    MarkdownContentImporter markdownContentImporter(MarkdownImportRegistry registry) {
        return new MarkdownContentImporter(registry, MAX_CHUNK_LENGTH);
    }
}
