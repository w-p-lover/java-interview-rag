package com.javainterview.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = JavaInterviewRagApplication.class)
class JavaInterviewRagApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void usesLocalProfileAndDocumentedDevelopmentPortByDefault() {
        assertEquals("local", environment.getProperty("spring.profiles.default"));
        assertEquals("18080", environment.getProperty("server.port"));
    }
}
