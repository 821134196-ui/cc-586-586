package org.community.bookbox.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.community.bookbox.model.Book;
import org.community.bookbox.model.BookCondition;
import org.community.bookbox.model.BookStatus;
import org.community.bookbox.model.Box;
import org.community.bookbox.model.Donor;
import org.community.bookbox.model.HandlingRecord;
import org.community.bookbox.model.HandlingType;
import org.community.bookbox.model.Loan;
import org.community.bookbox.model.LoanStatus;
import org.community.bookbox.model.LocationEvent;
import org.community.bookbox.model.LocationEventType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.List;

/** 图书漂流核心业务：捐赠登记、检查上架、借出、归还、逾期提醒。 */
@ApplicationScoped
public class BookService {

    @ConfigProperty(name = "bookbox.loan-days", defaultValue = "14")
    int loanDays;

    @Inject
    NotificationService notificationService;

    // ---------- 捐赠与上架 ----------

    /** 居民捐书登记：生成独立实体编号，状态为待检查。 */
    @Transactional
    public Book registerDonation(String title, String isbn, String author, BookCondition condition,
                                 String donorName, String donorContact, boolean anonymous, String note) {
        Donor donor = null;
        if (donorName != null && !donorName.isBlank()) {
            donor = new Donor();
            donor.name = donorName.trim();
            donor.contact = donorContact;
            donor.anonymous = anonymous;
            donor.persist();
        }

        Book book = new Book();
        book.title = requireText(title, "书名不能为空");
        book.isbn = normalizeIsbn(isbn);
        book.author = author;
        book.condition = condition != null ? condition : BookCondition.GOOD;
        book.status = BookStatus.PENDING_INSPECTION;
        book.donor = donor;
        // 先以唯一占位符持久化拿到自增 id，再生成正式编号（同事务多本书时占位符不能重复）
        book.code = "T" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        book.persist();
        book.flush();
        book.code = generateBookCode(book.id);

        record(book, LocationEventType.DONATED, null, null,
                donor == null ? "自助登记" : donor.name,
                note != null ? note : "捐赠登记，待管理员检查");
        return book;
    }

    /** 管理员检查并上架到指定箱位。箱满时拒绝。 */
    @Transactional
    public Book shelve(String bookCode, String boxCode, BookCondition inspectedCondition, String adminNote) {
        Book book = mustFindBook(bookCode);
        if (book.status != BookStatus.PENDING_INSPECTION) {
            throw ApiException.conflict("只有待检查的捐赠图书才能上架，当前状态: " + book.status);
        }
        Box box = mustFindBox(boxCode);
        assertBoxHasFreeSlot(box);

        if (inspectedCondition != null) {
            book.condition = inspectedCondition;
        }
        book.status = BookStatus.ON_SHELF;
        book.currentBox = box;
        record(book, LocationEventType.SHELVED, null, box, "管理员",
                adminNote != null ? adminNote : "检查通过，上架");
        return book;
    }

    // ---------- 借还 ----------

    /** 读者扫码借书：同一实体编号不能同时借给两个人。 */
    @Transactional
    public Loan borrow(String bookCode, String borrowerName, String borrowerContact) {
        Book book = mustFindBook(bookCode);
        if (book.status != BookStatus.ON_SHELF) {
            throw ApiException.conflict(book.status == BookStatus.BORROWED
                    ? "该编号图书已被借出，不能同时借给两个人"
                    : "该编号图书当前不可借阅，状态: " + book.status);
        }
        if (Loan.findActiveByBook(book) != null) {
            throw ApiException.conflict("该编号图书存在未归还的借阅记录，不能重复借出");
        }

        Loan loan = new Loan();
        loan.book = book;
        loan.borrowerName = requireText(borrowerName, "借阅人姓名不能为空");
        loan.borrowerContact = borrowerContact;
        loan.borrowedAt = LocalDateTime.now();
        loan.dueAt = loan.borrowedAt.plusDays(loanDays);
        loan.status = LoanStatus.ACTIVE;
        loan.persist();

        Box fromBox = book.currentBox;
        book.status = BookStatus.BORROWED;
        book.currentBox = null;
        record(book, LocationEventType.BORROWED, fromBox, null, loan.borrowerName,
                "借出，应还日期 " + loan.dueAt.toLocalDate());
        return loan;
    }

    /** 读者扫码还书：可放回原箱或另一只箱；箱满拒绝；破损留处理记录。 */
    @Transactional
    public Loan returnBook(String bookCode, String boxCode, boolean damaged, String note) {
        Book book = mustFindBook(bookCode);
        Loan loan = Loan.findActiveByBook(book);
        if (loan == null) {
            throw ApiException.conflict("该编号图书没有未归还的借阅记录");
        }
        Box box = mustFindBox(boxCode);
        assertBoxHasFreeSlot(box);

        loan.status = LoanStatus.RETURNED;
        loan.returnedAt = LocalDateTime.now();
        loan.returnBox = box;

        book.status = BookStatus.ON_SHELF;
        book.currentBox = box;

        boolean wasOverdue = loan.returnedAt.isAfter(loan.dueAt);
        StringBuilder eventNote = new StringBuilder("归还入箱");
        if (wasOverdue) {
            eventNote.append("（逾期 ").append(java.time.Duration.between(loan.dueAt, loan.returnedAt).toDays()).append(" 天）");
        }
        if (damaged) {
            eventNote.append("，归还时发现破损");
        }
        if (note != null && !note.isBlank()) {
            eventNote.append("：").append(note);
        }
        record(book, LocationEventType.RETURNED, null, box, loan.borrowerName, eventNote.toString());

        if (damaged) {
            addHandlingRecord(book, loan, HandlingType.DAMAGE,
                    note != null && !note.isBlank() ? note : "归还时发现破损，请管理员检查", "管理员");
        }
        return loan;
    }

    // ---------- 逾期 ----------

    /** 检查所有逾期未还的借阅：发送模拟提醒并留处理记录（同一借阅只记一次）。返回本次提醒条数。 */
    @Transactional
    public int checkOverdueAndRemind() {
        List<Loan> overdueLoans = Loan.list("status = ?1 and dueAt < ?2", LoanStatus.ACTIVE, LocalDateTime.now());
        int reminded = 0;
        for (Loan loan : overdueLoans) {
            if (HandlingRecord.existsForLoanAndType(loan, HandlingType.OVERDUE)) {
                continue;
            }
            Book book = loan.book;
            String message = String.format("您借阅的《%s》(编号 %s) 已于 %s 到期，请尽快归还至任一漂流箱。",
                    book.title, book.code, loan.dueAt.toLocalDate());
            notificationService.send(
                    loan.borrowerContact != null && !loan.borrowerContact.isBlank()
                            ? loan.borrowerContact : loan.borrowerName,
                    message);
            addHandlingRecord(book, loan, HandlingType.OVERDUE,
                    "逾期未还，已发送提醒。应还日期 " + loan.dueAt.toLocalDate() + "，借阅人 " + loan.borrowerName, "系统");
            reminded++;
        }
        return reminded;
    }

    // ---------- 查询辅助 ----------

    public Book mustFindBook(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("图书编号不能为空");
        }
        Book book = Book.findByCode(code.trim().toUpperCase());
        if (book == null) {
            throw ApiException.notFound("不存在编号为 " + code + " 的图书");
        }
        return book;
    }

    public Box mustFindBox(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("箱位编号不能为空");
        }
        Box box = Box.findByCode(code.trim().toUpperCase());
        if (box == null) {
            throw ApiException.notFound("不存在编号为 " + code + " 的箱位");
        }
        return box;
    }

    // ---------- 内部 ----------

    /** 实体编号：BK- + 6 位自增序号，全局唯一（数据库唯一约束兜底）。 */
    static String generateBookCode(long id) {
        return "BK-" + String.format("%06d", id);
    }

    private void assertBoxHasFreeSlot(Box box) {
        if (!box.hasFreeSlot()) {
            throw ApiException.conflict(String.format("箱位 %s（%s）已满（%d/%d），无法继续上架或归还",
                    box.code, box.name, box.currentLoad(), box.capacity));
        }
    }

    private void record(Book book, LocationEventType type, Box from, Box to, String actor, String note) {
        LocationEvent event = new LocationEvent();
        event.book = book;
        event.type = type;
        event.fromBox = from;
        event.toBox = to;
        event.actor = actor;
        event.note = note;
        event.persist();
    }

    private void addHandlingRecord(Book book, Loan loan, HandlingType type, String note, String handledBy) {
        HandlingRecord record = new HandlingRecord();
        record.book = book;
        record.loan = loan;
        record.type = type;
        record.note = note;
        record.handledBy = handledBy;
        record.persist();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        String cleaned = isbn.replaceAll("[^0-9Xx]", "");
        return cleaned.isEmpty() ? null : cleaned.toUpperCase();
    }
}
