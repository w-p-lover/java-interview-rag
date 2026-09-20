package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.persistence.MySqlResumeCenterStore;
import com.javainterview.rag.interview.persistence.ResumeCenterMapper;
import com.javainterview.rag.interview.resume.ResumeCenterService;
import com.javainterview.rag.interview.resume.ResumeCenterStore;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ResumeCenterConfiguration {

    @Bean
    ResumeCenterStore resumeCenterStore(
            ResumeCenterMapper mapper,
            TransactionTemplate transactions) {
        return new MySqlResumeCenterStore(mapper, transactions);
    }

    @Bean
    ResumeCenterService resumeCenterService(
            ResumeCenterStore store,
            PdfKnowledgeDocumentExtractor pdfExtractor) {
        return new ResumeCenterService(store, pdfExtractor);
    }
}
