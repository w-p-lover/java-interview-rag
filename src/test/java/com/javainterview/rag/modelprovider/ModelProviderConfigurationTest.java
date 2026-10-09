package com.javainterview.rag.modelprovider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelProviderConfigurationTest {

    private final ModelProviderConfiguration configuration = new ModelProviderConfiguration();

    @Test
    void appliesIndependentChatResponseTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat", exchange -> {
            try {
                Thread.sleep(300);
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
            RestClient client = configuration.modelProviderChatRestClientBuilder(
                            Duration.ofSeconds(1), Duration.ofMillis(50))
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .build();
            long startedAt = System.nanoTime();

            assertThrows(ResourceAccessException.class, () -> client.get()
                    .uri("/chat")
                    .retrieve()
                    .body(String.class));

            assertTrue(Duration.ofNanos(System.nanoTime() - startedAt)
                    .compareTo(Duration.ofSeconds(1)) < 0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void interruptsChatHttpWaitBeforeProviderResponds() throws Exception {
        var received = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var finished = new java.util.concurrent.CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat", exchange -> {
            received.countDown();
            try {
                release.await(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            RestClient client = configuration.modelProviderChatRestClientBuilder(
                            Duration.ofSeconds(1), Duration.ofSeconds(10))
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            var future = executor.submit(() -> {
                try {
                    client.get().uri("/chat").retrieve().body(String.class);
                } catch (ResourceAccessException expected) {
                    // 取消请求后 HTTP 等待以中断异常退出。
                } finally {
                    finished.countDown();
                }
            });
            assertTrue(received.await(1, java.util.concurrent.TimeUnit.SECONDS));
            future.cancel(true);
            assertTrue(finished.await(1, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
            server.stop(0);
        }
    }

    @Test
    void rejectsNonPositiveChatTimeouts() {
        assertThrows(IllegalArgumentException.class,
                () -> configuration.modelProviderChatRestClientBuilder(
                        Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> configuration.modelProviderChatRestClientBuilder(
                        Duration.ofSeconds(1), Duration.ZERO));
    }
}
