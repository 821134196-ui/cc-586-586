package org.community.bookbox;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.community.bookbox.model.Book;
import org.community.bookbox.model.BookCondition;
import org.community.bookbox.model.Box;
import org.community.bookbox.service.BookService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/** 首次启动时写入演示数据（箱位、图书、一条在借记录）。 */
@ApplicationScoped
public class SeedData {

    private static final Logger LOG = Logger.getLogger(SeedData.class);

    @ConfigProperty(name = "bookbox.seed.enabled", defaultValue = "true")
    boolean seedEnabled;

    @Inject
    BookService bookService;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!seedEnabled || Box.count() > 0) {
            return;
        }
        LOG.info("初始化演示数据...");

        Box boxA = new Box();
        boxA.code = "BOX-A";
        boxA.name = "社区中心漂流箱";
        boxA.location = "社区活动中心一楼大厅";
        boxA.capacity = 5;
        boxA.persist();

        Box boxB = new Box();
        boxB.code = "BOX-B";
        boxB.name = "花园角漂流箱";
        boxB.location = "中心花园凉亭旁";
        boxB.capacity = 3;
        boxB.persist();

        // 同一 ISBN 的两册《三体》，分别获得独立实体编号
        Book santi1 = bookService.registerDonation("三体", "9787536692930", "刘慈欣",
                BookCondition.GOOD, "张伟", "13800000001", false, null);
        Book santi2 = bookService.registerDonation("三体", "9787536692930", "刘慈欣",
                BookCondition.FAIR, "匿名", null, true, null);
        Book huozhe = bookService.registerDonation("活着", "9787506365437", "余华",
                BookCondition.GOOD, "李芳", "13800000002", false, null);
        Book pending = bookService.registerDonation("人类简史", "9787508647357", "尤瓦尔·赫拉利",
                BookCondition.NEW, "匿名", null, true, "书况很好，待检查");

        bookService.shelve(santi1.code, "BOX-A", null, null);
        bookService.shelve(santi2.code, "BOX-A", null, null);
        bookService.shelve(huozhe.code, "BOX-B", null, null);

        bookService.borrow(santi1.code, "王强", "13800000003");

        LOG.infof("演示数据就绪：%s / %s 在 BOX-A，%s 在 BOX-B，%s 待上架",
                santi1.code, santi2.code, huozhe.code, pending.code);
    }
}
