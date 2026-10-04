package org.bookbox;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.bookbox.model.Loan;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心业务规则验证：
 * 1. 实体编号唯一
 * 2. 箱容量限制（箱满不能继续上架）
 * 3. 跨箱归还后位置正确、流转可追查
 * 4. 同 ISBN 两册可分别借出；同一实体编号不能同时借给两人
 * 5. 匿名捐赠者身份不对读者展示
 * 6. 逾期 / 破损留处理记录
 */
@QuarkusTest
class LibraryRulesTest {

    // ---------- 辅助 ----------

    private void createBox(String code, int capacity) {
        given().contentType(ContentType.JSON)
                .body(Map.of("code", code, "name", "测试箱" + code,
                        "location", "测试点", "capacity", capacity))
                .when().post("/api/admin/boxes")
                .then().statusCode(200);
    }

    /** 登记捐赠，返回 [donationId, copyCode] */
    private String[] donate(String title, String isbn, String donor, boolean anonymous) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("author", "作者");
        body.put("isbn", isbn);
        body.put("condition", "GOOD");
        body.put("donorName", donor);
        body.put("anonymous", anonymous);
        Response resp = given().contentType(ContentType.JSON).body(body)
                .when().post("/api/admin/donations")
                .then().statusCode(200).extract().response();
        return new String[]{resp.jsonPath().getString("id"), resp.jsonPath().getString("copyCode")};
    }

    private Response approve(String donationId, String boxCode) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("boxCode", boxCode))
                .when().post("/api/admin/donations/" + donationId + "/approve")
                .then().extract().response();
    }

    private Response borrow(String copyCode, String borrower) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("copyCode", copyCode, "borrowerName", borrower))
                .when().post("/api/loans")
                .then().extract().response();
    }

    private Response returnBook(String copyCode, String boxCode, String damageNote) {
        Map<String, Object> body = new HashMap<>();
        body.put("copyCode", copyCode);
        body.put("boxCode", boxCode);
        body.put("damageNote", damageNote);
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/api/loans/return")
                .then().extract().response();
    }

    // ---------- 1. 实体编号唯一 ----------

    @Test
    void copyCodesAreGloballyUnique() {
        String[] a = donate("编号唯一测试书甲", "ISBN-U1", "甲", false);
        String[] b = donate("编号唯一测试书乙", "ISBN-U1", "乙", false); // 同 ISBN
        String[] c = donate("编号唯一测试书甲", "ISBN-U2", "丙", false); // 同书名

        Set<String> codes = new HashSet<>(List.of(a[1], b[1], c[1]));
        assertEquals(3, codes.size(), "同书名/同 ISBN 的实体书也必须各有独立编号");
        for (String code : codes) {
            assertTrue(code.matches("BK-\\d{6}"), "编号格式应为 BK-XXXXXX: " + code);
        }
    }

    // ---------- 2. 箱容量限制 ----------

    @Test
    void fullBoxRejectsShelving() {
        createBox("T-CAP", 1);
        String[] first = donate("容量测试书一", "ISBN-C1", "甲", false);
        String[] second = donate("容量测试书二", "ISBN-C2", "乙", false);

        assertEquals(200, approve(first[0], "T-CAP").statusCode());

        Response overflow = approve(second[0], "T-CAP");
        assertEquals(409, overflow.statusCode(), "箱满后必须拒绝继续上架");
        assertThat(overflow.jsonPath().getString("error"), containsString("已满"));

        // 箱占用数仍为 1
        given().when().get("/api/boxes").then().statusCode(200)
                .body("find { it.code == 'T-CAP' }.occupied", equalTo(1))
                .body("find { it.code == 'T-CAP' }.capacity", equalTo(1));
    }

    @Test
    void returnToFullBoxIsRejected() {
        createBox("T-FULL", 1);
        createBox("T-SPARE", 5);
        String[] occupant = donate("占位书", "ISBN-F1", "甲", false);
        approve(occupant[0], "T-FULL");

        String[] roaming = donate("漂流书", "ISBN-F2", "乙", false);
        approve(roaming[0], "T-SPARE");
        assertEquals(200, borrow(roaming[1], "读者甲").statusCode());

        Response resp = returnBook(roaming[1], "T-FULL", null);
        assertEquals(409, resp.statusCode(), "归还到已满的箱必须被拒绝");
    }

    // ---------- 3. 跨箱归还 ----------

    @Test
    void crossBoxReturnUpdatesLocationAndKeepsFullTrace() {
        createBox("T-FROM", 5);
        createBox("T-TO", 5);
        String[] donation = donate("跨箱漂流书", "ISBN-X1", "甲", false);
        approve(donation[0], "T-FROM");
        String copyCode = donation[1];

        assertEquals(200, borrow(copyCode, "读者乙").statusCode());
        // 借出后不在任何箱
        given().when().get("/api/copies/" + copyCode).then()
                .body("status", equalTo("BORROWED"))
                .body("boxCode", nullValue());

        // 还到另一只箱
        assertEquals(200, returnBook(copyCode, "T-TO", null).statusCode());

        // 当前位置更新为目标箱
        given().when().get("/api/copies/" + copyCode).then()
                .body("status", equalTo("ON_SHELF"))
                .body("boxCode", equalTo("T-TO"));

        // 完整流转可追查：上架(T-FROM) → 借出(from T-FROM) → 归还(to T-TO)
        given().when().get("/api/copies/" + copyCode + "/history").then()
                .body("type", contains("DONATION_REGISTERED", "SHELVED", "BORROWED", "RETURNED"))
                .body("find { it.type == 'SHELVED' }.toBox", equalTo("T-FROM"))
                .body("find { it.type == 'BORROWED' }.fromBox", equalTo("T-FROM"))
                .body("find { it.type == 'RETURNED' }.toBox", equalTo("T-TO"));
    }

    // ---------- 4. 借出规则 ----------

    @Test
    void sameCopyCannotBeLentToTwoPeople() {
        createBox("T-ONE", 5);
        String[] donation = donate("单本借出测试", "ISBN-L1", "甲", false);
        approve(donation[0], "T-ONE");

        assertEquals(200, borrow(donation[1], "读者一").statusCode());
        Response second = borrow(donation[1], "读者二");
        assertEquals(409, second.statusCode(), "同一实体编号不能同时借给两个人");
    }

    @Test
    void sameIsbnCopiesCanBeLentSeparately() {
        createBox("T-ISBN", 5);
        String[] copyA = donate("同ISBN书", "ISBN-SAME", "甲", false);
        String[] copyB = donate("同ISBN书", "ISBN-SAME", "乙", false);
        approve(copyA[0], "T-ISBN");
        approve(copyB[0], "T-ISBN");
        assertNotEquals(copyA[1], copyB[1]);

        assertEquals(200, borrow(copyA[1], "读者一").statusCode());
        assertEquals(200, borrow(copyB[1], "读者二").statusCode(),
                "ISBN 相同的不同实体书应可分别借出");
    }

    // ---------- 5. 匿名捐赠者 ----------

    @Test
    void anonymousDonorHiddenFromReaders() {
        createBox("T-ANON", 5);
        String secretName = "隐秘捐赠者姓名";
        String[] donation = donate("匿名捐赠测试书", "ISBN-A1", secretName, true);
        approve(donation[0], "T-ANON");

        String detail = given().when().get("/api/copies/" + donation[1]).then()
                .statusCode(200).extract().asString();
        assertFalse(detail.contains(secretName), "读者查书接口不得泄露捐赠者");

        String history = given().when().get("/api/copies/" + donation[1] + "/history").then()
                .statusCode(200).extract().asString();
        assertFalse(history.contains(secretName), "流转轨迹不得泄露匿名捐赠者");

        String search = given().when().get("/api/books/search?q=匿名捐赠测试书").then()
                .statusCode(200).extract().asString();
        assertFalse(search.contains(secretName), "检索结果不得泄露匿名捐赠者");

        // 管理端可见（标注为匿名）
        given().when().get("/api/admin/donations?status=APPROVED").then()
                .body("find { it.copyCode == '" + donation[1] + "' }.donorName",
                        equalTo("匿名捐赠者"))
                .body("find { it.copyCode == '" + donation[1] + "' }.anonymous",
                        equalTo(true));
    }

    // ---------- 6. 逾期与破损处理记录 ----------

    @Test
    void overdueScanCreatesIncidentAndSendsMockReminder() {
        createBox("T-OD", 5);
        String[] donation = donate("逾期测试书", "ISBN-O1", "甲", false);
        approve(donation[0], "T-OD");
        String copyCode = donation[1];
        assertEquals(200, borrow(copyCode, "逾期读者").statusCode());

        // 将应还时间改到昨天（模拟逾期）
        QuarkusTransaction.requiringNew().run(() -> {
            Loan loan = Loan.findActiveByCopy(org.bookbox.model.BookCopy.findByCopyCode(copyCode));
            loan.dueAt = Instant.now().minus(1, ChronoUnit.DAYS);
        });

        given().when().post("/api/admin/reminders/run").then()
                .statusCode(200)
                .body("created", greaterThanOrEqualTo(1));

        given().when().get("/api/admin/incidents?open=true").then()
                .body("find { it.copyCode == '" + copyCode + "' }.type", equalTo("OVERDUE"))
                .body("find { it.copyCode == '" + copyCode + "' }.resolved", equalTo(false));

        // 模拟提醒接口已写出通知
        java.nio.file.Path log = java.nio.file.Path.of("data", "notifications.log");
        assertTrue(java.nio.file.Files.exists(log), "模拟提醒应写入 notifications.log");
    }

    @Test
    void damagedReturnCreatesIncidentAndSkipsShelf() {
        createBox("T-DMG", 1);
        createBox("T-DMG2", 5);
        String[] occupant = donate("占位书D", "ISBN-D1", "甲", false);
        approve(occupant[0], "T-DMG"); // T-DMG 已满

        String[] fragile = donate("易碎的书", "ISBN-D2", "乙", false);
        approve(fragile[0], "T-DMG2");
        assertEquals(200, borrow(fragile[1], "读者丙").statusCode());

        // 箱虽满，破损归还不占箱位，应成功并留处理记录
        assertEquals(200, returnBook(fragile[1], "T-DMG", "封面撕裂").statusCode());

        given().when().get("/api/copies/" + fragile[1]).then()
                .body("status", equalTo("DAMAGED"))
                .body("boxCode", nullValue());

        given().when().get("/api/admin/incidents").then()
                .body("find { it.copyCode == '" + fragile[1] + "' && it.type == 'DAMAGE' }.note",
                        containsString("封面撕裂"))
                .body("find { it.copyCode == '" + fragile[1] + "' && it.type == 'DAMAGE' }.resolved",
                        equalTo(false));

        // 修复后重新上架到有空位的箱
        given().contentType(ContentType.JSON)
                .body(Map.of("boxCode", "T-DMG2", "note", "已修补"))
                .when().post("/api/admin/copies/" + fragile[1] + "/reshelve")
                .then().statusCode(200)
                .body("status", equalTo("ON_SHELF"))
                .body("boxCode", equalTo("T-DMG2"));
    }
}
