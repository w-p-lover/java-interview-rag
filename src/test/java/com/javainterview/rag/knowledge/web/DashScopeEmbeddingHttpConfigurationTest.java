package com.javainterview.rag.knowledge.web;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeEmbeddingHttpConfigurationTest {

    private final DashScopeEmbeddingHttpConfiguration configuration =
            new DashScopeEmbeddingHttpConfiguration();

    @Test
    void stopsWaitingWhenProviderResponseExceedsDeadline() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embedding", exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("ok".getBytes());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            RestClient client = configuration.dashScopeEmbeddingRestClientBuilder(
                            Duration.ofSeconds(1), Duration.ofMillis(50))
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .build();
            long startedAt = System.nanoTime();

            assertThrows(ResourceAccessException.class, () -> client.get()
                    .uri("/embedding")
                    .retrieve()
                    .body(String.class));

            assertTrue(Duration.ofNanos(System.nanoTime() - startedAt).compareTo(Duration.ofSeconds(1)) < 0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void embeddingRequestIsNotRetriedAfterTimeout() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ResourceAccessException.class, () -> configuration.dashScopeEmbeddingRetryTemplate()
                .execute(context -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("timeout");
                }));

        assertEquals(1, attempts.get());
    }

    @Test
    void rejectsNonPositiveTimeouts() {
        assertThrows(IllegalArgumentException.class,
                () -> configuration.dashScopeEmbeddingRestClientBuilder(Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> configuration.dashScopeEmbeddingRestClientBuilder(Duration.ofSeconds(1), Duration.ZERO));
    }
}
