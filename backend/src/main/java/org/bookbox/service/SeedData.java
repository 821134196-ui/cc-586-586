package org.bookbox.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bookbox.model.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 首次启动（空库）时写入演示数据 */
@ApplicationScoped
public class SeedData {

    @ConfigProperty(name = "bookbox.seed.enabled", defaultValue = "true")
    boolean seedEnabled;

    @Inject
    LibraryService library;

    void onStart(@Observes StartupEvent event) {
        if (!seedEnabled || BookBox.count() > 0) {
            return;
        }
        QuarkusTransaction.run(this::seed);
    }

    private void seed() {
        library.createBox("BOX-A", "社区服务中心漂流箱", "服务中心一楼大厅", 4);
        library.createBox("BOX-B", "中心花园漂流箱", "中心花园凉亭旁", 2);
        library.createBox("BOX-C", "文化活动室漂流箱", "活动室书架旁", 3);

        // 上架一批书（含同 ISBN 两册）
        shelve("活着", "余华", "9787506365437", "BOX-A", "王阿姨", false);
        shelve("活着", "余华", "9787506365437", "BOX-A", "匿名", true);
        shelve("人类简史", "尤瓦尔·赫拉利", "9787508647357", "BOX-A", "李先生", false);
        shelve("小王子", "圣埃克苏佩里", "9787020042494", "BOX-B", "匿名", true);
        shelve("三体", "刘慈欣", "9787536692930", "BOX-C", "张同学", false);

        // 一笔正常在借
        Donation d6 = library.registerDonation("平凡的世界", "路遥", "9787530216781",
                BookCondition.GOOD, "陈女士", false, null);
        library.approveDonation(d6.id, "BOX-C");
        Loan active = library.borrow(copyCodeOf(d6), "赵读者", "13800000001");
        active.borrowedAt = Instant.now().minus(5, ChronoUnit.DAYS);
        active.dueAt = Instant.now().plus(25, ChronoUnit.DAYS);

        // 一笔逾期在借（演示逾期处理记录与提醒）
        Donation d7 = library.registerDonation("围城", "钱钟书", "9787020024759",
                BookCondition.WORN, "匿名", true, null);
        library.approveDonation(d7.id, "BOX-C");
        Loan overdue = library.borrow(copyCodeOf(d7), "钱读者", "13800000002");
        overdue.borrowedAt = Instant.now().minus(45, ChronoUnit.DAYS);
        overdue.dueAt = Instant.now().minus(15, ChronoUnit.DAYS);

        // 一笔跨箱归还历史：A 箱借出，还到 B 箱
        Donation d8 = library.registerDonation("月亮与六便士", "毛姆", "9787533936020",
                BookCondition.GOOD, "孙女士", false, null);
        library.approveDonation(d8.id, "BOX-A");
        library.borrow(copyCodeOf(d8), "周读者", null);
        library.returnBook(copyCodeOf(d8), "BOX-B", null);

        // 一笔待审核捐赠
        library.registerDonation("解忧杂货店", "东野圭吾", "9787544270878",
                BookCondition.NEW, "匿名", true, "九成新，希望漂流起来");
    }

    private void shelve(String title, String author, String isbn, String boxCode,
                        String donor, boolean anonymous) {
        Donation d = library.registerDonation(title, author, isbn, BookCondition.GOOD,
                donor, anonymous, null);
        library.approveDonation(d.id, boxCode);
    }

    private String copyCodeOf(Donation donation) {
        return Donation.<Donation>findById(donation.id).bookCopy.copyCode;
    }
}
