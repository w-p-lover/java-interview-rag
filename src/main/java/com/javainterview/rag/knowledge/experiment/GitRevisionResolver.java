package com.javainterview.rag.knowledge.experiment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class GitRevisionResolver {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    private GitRevisionResolver() {
    }

    public static String resolve(String configured) {
        if (configured != null && !configured.isBlank() && !"auto".equalsIgnoreCase(configured.strip())) {
            return configured.strip();
        }
        try {
            String revision = command(true, "git", "rev-parse", "--short=12", "HEAD");
            boolean dirty = !command(false, "git", "status", "--porcelain", "--untracked-files=no").isBlank();
            return dirty ? revision + "-dirty" : revision;
        } catch (RuntimeException exception) {
            return "unversioned-" + UUID.randomUUID();
        }
    }

    private static String command(boolean requireOutput, String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Git 版本命令超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (process.exitValue() != 0 || requireOutput && output.isBlank()) {
                throw new IllegalStateException("Git 版本命令失败");
            }
            return output;
        } catch (IOException exception) {
            throw new IllegalStateException("Git 版本命令不可用", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git 版本命令被中断", exception);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
