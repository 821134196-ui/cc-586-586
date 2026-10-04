package org.bookbox.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * 模拟提醒接口：不真正发送短信/推送，
 * 将提醒内容追加到数据目录的 notifications.log 并打印日志。
 */
@ApplicationScoped
public class MockNotificationService {

    @ConfigProperty(name = "bookbox.data-dir", defaultValue = "data")
    String dataDir;

    public synchronized void send(String recipient, String message) {
        String line = "[%s] TO=%s MSG=%s%n".formatted(Instant.now(), recipient, message);
        try {
            Path log = Path.of(dataDir, "notifications.log");
            Files.createDirectories(log.getParent());
            Files.writeString(log, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.print("[MOCK-NOTIFY] " + line);
    }
}
