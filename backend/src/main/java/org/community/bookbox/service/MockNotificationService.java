package org.community.bookbox.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * 模拟通知接口：不真正外发，写日志并追加到本地文件 data/notifications.log，
 * 便于验证逾期提醒流程。
 */
@ApplicationScoped
public class MockNotificationService implements NotificationService {

    private static final Logger LOG = Logger.getLogger(MockNotificationService.class);

    @ConfigProperty(name = "bookbox.data-dir", defaultValue = "data")
    String dataDir;

    @Override
    public void send(String target, String message) {
        String line = String.format("%s\t-> %s\t%s%n", LocalDateTime.now(), target, message);
        LOG.infof("[模拟提醒] 发送给 %s: %s", target, message);
        try {
            Path log = Path.of(dataDir, "notifications.log");
            Files.createDirectories(log.getParent());
            Files.writeString(log, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warnf(e, "写入模拟提醒日志失败");
        }
    }
}
