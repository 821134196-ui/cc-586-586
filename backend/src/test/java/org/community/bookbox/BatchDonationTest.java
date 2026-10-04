package org.community.bookbox;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.community.bookbox.model.Book;
import org.community.bookbox.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 回归：同一事务内连续登记多本捐赠（如启动种子数据），实体编号仍各自唯一 */
@QuarkusTest
class BatchDonationTest {

    @Inject
    BookService bookService;

    @Test
    @DisplayName("同一事务内登记多本捐赠，编号不冲突")
    void multipleDonationsInOneTransactionGetUniqueCodes() {
        String[] codes = QuarkusTransaction.requiringNew().call(() -> {
            Book a = bookService.registerDonation("批量书甲", "9780000000001", null, null, null, null, false, null);
            Book b = bookService.registerDonation("批量书乙", "9780000000002", null, null, null, null, false, null);
            Book c = bookService.registerDonation("批量书丙", "9780000000003", null, null, null, null, false, null);
            return new String[]{a.code, b.code, c.code};
        });

        assertEquals(3, java.util.Arrays.stream(codes).distinct().count(), "三本书编号必须互不相同");
        for (String code : codes) {
            assertTrue(code.matches("BK-\\d{6}"), "编号格式应为 BK-XXXXXX，实际: " + code);
        }
    }
}
