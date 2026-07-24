package com.javainterview.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;

@SpringBootApplication(exclude = {
        OpenAiChatAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class
})
public class JavaInterviewRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaInterviewRagApplication.class, args);
    }
}
