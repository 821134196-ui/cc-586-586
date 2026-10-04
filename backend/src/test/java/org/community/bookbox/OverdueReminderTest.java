package org.community.bookbox;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.community.bookbox.model.Loan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** 逾期提醒：模拟接口发送 + 处理记录留痕 */
@QuarkusTest
class OverdueReminderTest {

    @Test
    @DisplayName("逾期借阅触发模拟提醒并生成 OVERDUE 处理记录，且不重复记录")
    void overdueLoanTriggersReminderAndHandlingRecord() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String boxCode = "OV" + suffix.substring(0, 6);

        given().contentType(ContentType.JSON)
                .body(Map.of("code", boxCode, "name", "逾期测试箱", "capacity", 5))
                .when().post("/api/boxes").then().statusCode(200);

        String bookCode = given().contentType(ContentType.JSON)
                .body(Map.of("title", "逾期测试书", "isbn", "9781111111111"))
                .when().post("/api/donations").then().statusCode(201)
                .extract().path("code");

        given().contentType(ContentType.JSON).body(Map.of("boxCode", boxCode))
                .when().post("/api/books/{code}/shelve", bookCode).then().statusCode(200);

        long loanId = Long.parseLong(given().contentType(ContentType.JSON)
                .body(Map.of("bookCode", bookCode, "borrowerName", "逾期读者", "borrowerContact", "13600000000"))
                .when().post("/api/borrow").then().statusCode(200)
                .extract().path("id").toString());

        // 把应还日期改到昨天，模拟逾期
        QuarkusTransaction.requiringNew().run(() -> {
            Loan loan = Loan.findById(loanId);
            loan.dueAt = LocalDateTime.now().minusDays(1);
        });

        // 借阅列表中标记为逾期
        given().get("/api/loans?active=true").then()
                .body("find { it.id == %d }.overdue".formatted(loanId), equalTo(true));

        // 触发提醒
        given().post("/api/admin/remind-overdue").then().statusCode(200)
                .body("reminded", greaterThanOrEqualTo(1));

        // 处理记录已生成
        given().get("/api/admin/handling-records").then()
                .body("find { it.loanId == %d }.type".formatted(loanId), equalTo("OVERDUE"));

        // 再次触发不重复记录
        int before = given().get("/api/admin/handling-records").then().extract()
                .jsonPath().getList("findAll { it.loanId == %d }".formatted(loanId)).size();
        given().post("/api/admin/remind-overdue").then().statusCode(200);
        int after = given().get("/api/admin/handling-records").then().extract()
                .jsonPath().getList("findAll { it.loanId == %d }".formatted(loanId)).size();
        org.junit.jupiter.api.Assertions.assertEquals(before, after, "同一逾期借阅不应重复生成处理记录");
    }
}
