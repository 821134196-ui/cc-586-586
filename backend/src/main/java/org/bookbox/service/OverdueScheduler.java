package org.bookbox.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** 定时扫描逾期并发送模拟提醒；也可通过管理接口手动触发 */
@ApplicationScoped
public class OverdueScheduler {

    @Inject
    LibraryService library;

    @Scheduled(every = "12h", delayed = "2m")
    void scan() {
        int created = library.scanOverdueAndNotify();
        if (created > 0) {
            System.out.println("[scheduler] 新建逾期处理记录 " + created + " 条");
        }
    }
}
