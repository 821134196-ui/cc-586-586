package org.community.bookbox;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 需求要求的验证点：
 * 1. 实体编号唯一（同 ISBN 两册分别借出）
 * 2. 箱容量限制（箱满拒绝上架）
 * 3. 跨箱归还后所在位置更新且流转历史完整
 * 另覆盖：同一实体不能同时借给两人、破损归还留处理记录、匿名捐赠者对读者隐藏。
 */
@QuarkusTest
class BookFlowTest {

    // ---------- 辅助 ----------

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String createBox(int capacity) {
        String code = unique("T");
        given().contentType(ContentType.JSON)
                .body(Map.of("code", code, "name", "测试箱" + code, "location", "测试点", "capacity", capacity))
                .when().post("/api/boxes")
                .then().statusCode(200)
                .body("capacity", equalTo(capacity));
        return code;
    }

    private String donate(String title, String isbn, boolean anonymous) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("title", title, "isbn", isbn, "author", "作者",
                        "condition", "GOOD", "donorName", "测试捐赠者",
                        "donorContact", "13900000000", "anonymous", anonymous))
                .when().post("/api/donations")
                .then().statusCode(201)
                .body("code", matchesPattern("BK-\\d{6}"))
                .extract().path("code");
    }

    private void shelve(String bookCode, String boxCode, int expectedStatus) {
        given().contentType(ContentType.JSON)
                .body(Map.of("boxCode", boxCode))
                .when().post("/api/books/{code}/shelve", bookCode)
                .then().statusCode(expectedStatus);
    }

    private void borrow(String bookCode, String borrower, int expectedStatus) {
        given().contentType(ContentType.JSON)
                .body(Map.of("bookCode", bookCode, "borrowerName", borrower, "borrowerContact", "13700000000"))
                .when().post("/api/borrow")
                .then().statusCode(expectedStatus);
    }

    // ---------- 验证点 1：实体编号唯一 ----------

    @Test
    @DisplayName("同 ISBN 两册书获得不同实体编号，且可分别借出")
    void sameIsbnTwoCopiesGetUniqueCodesAndBorrowIndependently() {
        String box = createBox(10);
        String isbn = "9787020002207";

        String copy1 = donate("围城", isbn, false);
        String copy2 = donate("围城", isbn, false);

        assertNotEquals(copy1, copy2, "同一 ISBN 的两册必须分配不同实体编号");

        shelve(copy1, box, 200);
        shelve(copy2, box, 200);

        // 两册可分别借给两个人
        borrow(copy1, "读者甲", 200);
        borrow(copy2, "读者乙", 200);

        given().get("/api/books/{code}", copy1).then()
                .body("status", equalTo("BORROWED"));
        given().get("/api/books/{code}", copy2).then()
                .body("status", equalTo("BORROWED"));
    }

    @Test
    @DisplayName("同一实体编号不能同时借给两个人")
    void sameCopyCannotBeBorrowedTwice() {
        String box = createBox(10);
        String book = donate("活着", "9787506365437", false);
        shelve(book, box, 200);

        borrow(book, "读者甲", 200);
        // 第二次借出同一实体编号 → 409
        given().contentType(ContentType.JSON)
                .body(Map.of("bookCode", book, "borrowerName", "读者乙"))
                .when().post("/api/borrow")
                .then().statusCode(409)
                .body("error", containsString("不能同时借给两个人"));
    }

    // ---------- 验证点 2：箱容量限制 ----------

    @Test
    @DisplayName("箱位满了不能继续上架")
    void fullBoxRejectsShelving() {
        String box = createBox(1);
        String book1 = donate("平凡的世界", "9787530216781", false);
        String book2 = donate("白鹿原", "9787020026616", false);

        shelve(book1, box, 200);
        // 第二本上架到已满的箱 → 409
        given().contentType(ContentType.JSON)
                .body(Map.of("boxCode", box))
                .when().post("/api/books/{code}/shelve", book2)
                .then().statusCode(409)
                .body("error", containsString("已满"));

        // 箱位占用显示 1/1
        given().get("/api/boxes").then()
                .body("find { it.code == '%s' }.load".formatted(box), equalTo(1))
                .body("find { it.code == '%s' }.free".formatted(box), equalTo(0));
    }

    // ---------- 验证点 3：跨箱归还后的所在位置 ----------

    @Test
    @DisplayName("跨箱归还后位置更新到新箱，流转历史完整可查")
    void crossBoxReturnUpdatesLocationAndKeepsHistory() {
        String boxA = createBox(5);
        String boxB = createBox(5);
        String book = donate("边城", "9787020001903", false);

        shelve(book, boxA, 200);
        given().get("/api/books/{code}", book).then()
                .body("status", equalTo("ON_SHELF"))
                .body("boxCode", equalTo(boxA));

        borrow(book, "读者甲", 200);
        given().get("/api/books/{code}", book).then()
                .body("status", equalTo("BORROWED"))
                .body("boxCode", nullValue());

        // 归还到另一只箱
        given().contentType(ContentType.JSON)
                .body(Map.of("bookCode", book, "boxCode", boxB, "damaged", false))
                .when().post("/api/returns")
                .then().statusCode(200)
                .body("status", equalTo("RETURNED"))
                .body("returnBoxCode", equalTo(boxB));

        // 当前所在：新箱
        given().get("/api/books/{code}", book).then()
                .body("status", equalTo("ON_SHELF"))
                .body("boxCode", equalTo(boxB));

        // 完整流转：捐赠 → 上架A → 借出(离开A) → 归还(进入B)
        List<Map<String, Object>> history = given().get("/api/books/{code}/history", book)
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        List<String> types = history.stream().map(h -> (String) h.get("type")).toList();
        assertEquals(List.of("DONATED", "SHELVED", "BORROWED", "RETURNED"), types);

        Map<String, Object> shelved = history.get(1);
        assertEquals(boxA, shelved.get("toBoxCode"));
        Map<String, Object> borrowed = history.get(2);
        assertEquals(boxA, borrowed.get("fromBoxCode"));
        Map<String, Object> returned = history.get(3);
        assertEquals(boxB, returned.get("toBoxCode"));
    }

    // ---------- 其他关键规则 ----------

    @Test
    @DisplayName("归还时发现破损会留下处理记录")
    void damagedReturnCreatesHandlingRecord() {
        String box = createBox(5);
        String book = donate("呐喊", "9787020002208", false);
        shelve(book, box, 200);
        borrow(book, "读者丙", 200);

        given().contentType(ContentType.JSON)
                .body(Map.of("bookCode", book, "boxCode", box, "damaged", true, "note", "封面撕裂"))
                .when().post("/api/returns")
                .then().statusCode(200);

        given().get("/api/admin/handling-records").then()
                .body("find { it.bookCode == '%s' }.type".formatted(book), equalTo("DAMAGE"))
                .body("find { it.bookCode == '%s' }.note".formatted(book), containsString("封面撕裂"));
    }

    @Test
    @DisplayName("匿名捐赠者身份不对读者展示，管理员可见")
    void anonymousDonorHiddenFromReaders() {
        String book = donate("朝花夕拾", "9787020002209", true);

        // 读者视图：不暴露捐赠者姓名与联系方式
        given().get("/api/books/{code}", book).then()
                .body("donorLabel", equalTo("匿名捐赠"));

        // 管理员视图：可见真实身份
        given().get("/api/books/{code}/admin", book).then()
                .body("donorName", equalTo("测试捐赠者"))
                .body("donorAnonymous", equalTo(true));
    }

    @Test
    @DisplayName("按编号查书：不会与同名同 ISBN 的其他副本混淆")
    void lookupByCodeFindsExactCopy() {
        String box = createBox(10);
        String isbn = "9787020009999";
        String copy1 = donate("重复书名", isbn, false);
        String copy2 = donate("重复书名", isbn, false);
        shelve(copy1, box, 200);
        shelve(copy2, box, 200);
        borrow(copy2, "读者丁", 200);

        // 按编号精确查询：copy1 仍在架，copy2 已借出
        given().get("/api/books/{code}", copy1).then()
                .body("code", equalTo(copy1))
                .body("status", equalTo("ON_SHELF"));
        given().get("/api/books/{code}", copy2).then()
                .body("code", equalTo(copy2))
                .body("status", equalTo("BORROWED"));
    }
}
