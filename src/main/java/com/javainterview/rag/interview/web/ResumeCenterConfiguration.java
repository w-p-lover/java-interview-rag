package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.MySqlResumeAnalysisResultStore;
import com.javainterview.rag.interview.persistence.MySqlResumeCenterStore;
import com.javainterview.rag.interview.persistence.ResumeAnalysisResultMapper;
import com.javainterview.rag.interview.persistence.ResumeCenterMapper;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultStore;
import com.javainterview.rag.interview.resume.ResumeCenterService;
import com.javainterview.rag.interview.resume.ResumeCenterStore;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import com.javainterview.rag.file.FileAssetService;
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
            PdfKnowledgeDocumentExtractor pdfExtractor,
            FileAssetService files) {
        return new ResumeCenterService(store, pdfExtractor, files);
    }

    @Bean
    ResumeAnalysisResultStore resumeAnalysisResultStore(ResumeAnalysisResultMapper mapper) {
        return new MySqlResumeAnalysisResultStore(mapper);
    }

    @Bean
    ResumeAnalysisResultService resumeAnalysisResultService(
            ResumeCenterStore resumeCenterStore,
            ResumeAnalysisResultStore resultStore,
            ObjectMapper objectMapper) {
        return new ResumeAnalysisResultService(resumeCenterStore, resultStore, objectMapper);
    }
}
