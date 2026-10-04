package org.bookbox.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bookbox.model.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 核心业务规则：
 * - 每本实体书分配全局唯一编号（数据库唯一约束兜底）
 * - 箱满拒绝上架
 * - 同一实体书同时只能借给一人
 * - 归还可入任意有空位的箱，全程留痕
 * - 逾期 / 破损生成处理记录
 */
@ApplicationScoped
public class LibraryService {

    @ConfigProperty(name = "bookbox.loan-days", defaultValue = "30")
    int loanDays;

    @Inject
    MockNotificationService notifications;

    // ---------- 箱 ----------

    @Transactional
    public BookBox createBox(String code, String name, String location, int capacity) {
        if (capacity < 1) {
            throw ApiException.badRequest("箱容量必须大于 0");
        }
        if (BookBox.findByCode(code) != null) {
            throw ApiException.conflict("箱编号已存在: " + code);
        }
        BookBox box = new BookBox();
        box.code = code;
        box.name = name;
        box.location = location;
        box.capacity = capacity;
        box.persist();
        return box;
    }

    // ---------- 捐赠 ----------

    /**
     * 登记捐赠：创建实体书（待审核状态）并分配唯一实体编号。
     * 编号在事务内先占位、flush 取到主键后生成 BK-XXXXXX，唯一约束兜底。
     */
    @Transactional
    public Donation registerDonation(String title, String author, String isbn,
                                     BookCondition condition, String donorName,
                                     boolean anonymous, String note) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("书名不能为空");
        }
        BookCopy copy = new BookCopy();
        copy.copyCode = "TMP-" + java.util.UUID.randomUUID();
        copy.title = title.trim();
        copy.author = author;
        copy.isbn = isbn;
        copy.bookCondition = condition == null ? BookCondition.GOOD : condition;
        copy.status = CopyStatus.PENDING_REVIEW;
        copy.persist();
        copy.flush(); // 取得自增主键
        copy.copyCode = "BK-%06d".formatted(copy.id);

        Donation donation = new Donation();
        donation.bookCopy = copy;
        donation.donorName = (donorName == null || donorName.isBlank()) ? null : donorName.trim();
        donation.anonymous = anonymous;
        donation.note = note;
        donation.persist();

        record(copy, MovementType.DONATION_REGISTERED, null, null,
                donation.anonymous ? "匿名捐赠者" : donation.donorName, "捐赠登记，待审核");
        return donation;
    }

    /** 审核通过并上架到指定箱；箱满则拒绝 */
    @Transactional
    public Donation approveDonation(long donationId, String boxCode) {
        Donation donation = Donation.findById(donationId);
        if (donation == null) {
            throw ApiException.notFound("捐赠记录不存在: " + donationId);
        }
        if (donation.status != DonationStatus.PENDING) {
            throw ApiException.conflict("该捐赠已审核，不能重复处理");
        }
        BookBox box = requireBox(boxCode);
        assertFreeSlot(box);

        BookCopy copy = donation.bookCopy;
        copy.status = CopyStatus.ON_SHELF;
        copy.currentBox = box;
        donation.status = DonationStatus.APPROVED;
        donation.reviewedAt = Instant.now();

        record(copy, MovementType.SHELVED, null, box, "管理员", "审核通过，上架 " + box.code);
        return donation;
    }

    @Transactional
    public Donation rejectDonation(long donationId, String note) {
        Donation donation = Donation.findById(donationId);
        if (donation == null) {
            throw ApiException.notFound("捐赠记录不存在: " + donationId);
        }
        if (donation.status != DonationStatus.PENDING) {
            throw ApiException.conflict("该捐赠已审核，不能重复处理");
        }
        donation.status = DonationStatus.REJECTED;
        donation.reviewedAt = Instant.now();
        donation.reviewNote = note;
        donation.bookCopy.status = CopyStatus.RETIRED;
        record(donation.bookCopy, MovementType.RETIRED, null, null, "管理员",
                "捐赠未通过审核" + (note == null ? "" : ": " + note));
        return donation;
    }

    // ---------- 借还 ----------

    /** 借出：必须在架，且同一实体编号不能同时借给两人 */
    @Transactional
    public Loan borrow(String copyCode, String borrowerName, String borrowerContact) {
        if (borrowerName == null || borrowerName.isBlank()) {
            throw ApiException.badRequest("借书人姓名不能为空");
        }
        BookCopy copy = requireCopy(copyCode);
        if (copy.status != CopyStatus.ON_SHELF) {
            throw ApiException.conflict("该书当前不可借（状态: " + copy.status + "）");
        }
        if (Loan.countActiveByCopy(copy) > 0) {
            throw ApiException.conflict("该实体书已被借出，不能同时借给两人");
        }
        BookBox fromBox = copy.currentBox;

        Loan loan = new Loan();
        loan.bookCopy = copy;
        loan.borrowerName = borrowerName.trim();
        loan.borrowerContact = borrowerContact;
        loan.borrowedAt = Instant.now();
        loan.dueAt = loan.borrowedAt.plus(loanDays, ChronoUnit.DAYS);
        loan.persist();

        copy.status = CopyStatus.BORROWED;
        copy.currentBox = null;

        record(copy, MovementType.BORROWED, fromBox, null, loan.borrowerName,
                "借出，应还 " + loan.dueAt);
        return loan;
    }

    /**
     * 归还：可还到任意有空位的箱（不限原箱）。
     * 归还时登记破损 → 书标记为破损待处理并生成处理记录。
     * 逾期归还需补建逾期处理记录并结案。
     */
    @Transactional
    public Loan returnBook(String copyCode, String boxCode, String damageNote) {
        BookCopy copy = requireCopy(copyCode);
        Loan loan = Loan.findActiveByCopy(copy);
        if (loan == null) {
            throw ApiException.conflict("该书没有未归还的借阅记录");
        }
        BookBox box = requireBox(boxCode);

        boolean damaged = damageNote != null && !damageNote.isBlank();
        if (!damaged) {
            // 完好归还才占用箱位；破损书先入待处理，不占箱位
            assertFreeSlot(box);
        }

        Instant now = Instant.now();
        loan.returnedAt = now;
        loan.returnBox = box;
        loan.damageNote = damaged ? damageNote.trim() : null;

        // 逾期：补建处理记录（若定时扫描尚未创建），归还即结案
        if (now.isAfter(loan.dueAt)) {
            IncidentRecord overdue = IncidentRecord.findOpen(IncidentType.OVERDUE, loan);
            if (overdue == null) {
                overdue = new IncidentRecord();
                overdue.type = IncidentType.OVERDUE;
                overdue.loan = loan;
                overdue.bookCopy = copy;
                overdue.note = "逾期归还（应还 " + loan.dueAt + "）";
                overdue.persist();
            }
            overdue.resolved = true;
            overdue.resolvedAt = now;
            overdue.handleNote = "读者已归还，逾期记录结案";
        }

        if (damaged) {
            copy.status = CopyStatus.DAMAGED;
            copy.currentBox = null;

            IncidentRecord damage = new IncidentRecord();
            damage.type = IncidentType.DAMAGE;
            damage.loan = loan;
            damage.bookCopy = copy;
            damage.note = "归还时发现破损: " + damageNote.trim();
            damage.persist();

            record(copy, MovementType.RETURNED, null, box, loan.borrowerName,
                    "归还至 " + box.code + "，发现破损，转入待处理");
            record(copy, MovementType.DAMAGED, box, null, "管理员", damageNote.trim());
        } else {
            copy.status = CopyStatus.ON_SHELF;
            copy.currentBox = box;
            record(copy, MovementType.RETURNED, null, box, loan.borrowerName,
                    "归还至 " + box.code);
        }
        return loan;
    }

    /** 破损书修复后重新上架 */
    @Transactional
    public BookCopy reshelve(String copyCode, String boxCode, String handleNote) {
        BookCopy copy = requireCopy(copyCode);
        if (copy.status != CopyStatus.DAMAGED) {
            throw ApiException.conflict("仅破损待处理的书可以重新上架");
        }
        BookBox box = requireBox(boxCode);
        assertFreeSlot(box);
        copy.status = CopyStatus.ON_SHELF;
        copy.currentBox = box;
        record(copy, MovementType.RESHELVED, null, box, "管理员",
                handleNote == null ? "修复后重新上架" : handleNote);
        return copy;
    }

    // ---------- 逾期扫描与提醒 ----------

    /** 扫描逾期未还，生成处理记录并通过模拟接口发送提醒。返回新建记录数。 */
    @Transactional
    public int scanOverdueAndNotify() {
        List<Loan> overdueLoans = Loan.list("returnedAt is null and dueAt < ?1", Instant.now());
        int created = 0;
        for (Loan loan : overdueLoans) {
            if (IncidentRecord.findOpen(IncidentType.OVERDUE, loan) != null) {
                continue;
            }
            IncidentRecord incident = new IncidentRecord();
            incident.type = IncidentType.OVERDUE;
            incident.loan = loan;
            incident.bookCopy = loan.bookCopy;
            incident.note = "逾期未还（应还 " + loan.dueAt + "）";
            incident.persist();
            created++;

            notifications.send(loan.borrowerName,
                    "您借阅的《" + loan.bookCopy.title + "》（编号 " + loan.bookCopy.copyCode
                            + "）已逾期，请尽快归还至任一漂流箱。");
        }
        return created;
    }

    @Transactional
    public IncidentRecord resolveIncident(long incidentId, String handleNote) {
        IncidentRecord incident = IncidentRecord.findById(incidentId);
        if (incident == null) {
            throw ApiException.notFound("处理记录不存在: " + incidentId);
        }
        if (incident.resolved) {
            throw ApiException.conflict("该记录已结案");
        }
        incident.resolved = true;
        incident.resolvedAt = Instant.now();
        incident.handleNote = handleNote;
        return incident;
    }

    // ---------- 工具 ----------

    public BookCopy requireCopy(String copyCode) {
        if (copyCode == null || copyCode.isBlank()) {
            throw ApiException.badRequest("实体编号不能为空");
        }
        BookCopy copy = BookCopy.findByCopyCode(copyCode.trim());
        if (copy == null) {
            throw ApiException.notFound("实体书不存在: " + copyCode);
        }
        return copy;
    }

    public BookBox requireBox(String boxCode) {
        if (boxCode == null || boxCode.isBlank()) {
            throw ApiException.badRequest("箱编号不能为空");
        }
        BookBox box = BookBox.findByCode(boxCode.trim());
        if (box == null) {
            throw ApiException.notFound("箱不存在: " + boxCode);
        }
        return box;
    }

    private void assertFreeSlot(BookBox box) {
        if (!box.hasFreeSlot()) {
            throw ApiException.conflict(
                    "箱 " + box.code + " 已满（" + BookBox.occupied(box) + "/" + box.capacity + "），无法继续上架");
        }
    }

    private void record(BookCopy copy, MovementType type, BookBox from, BookBox to,
                        String actor, String note) {
        MovementEvent event = new MovementEvent();
        event.bookCopy = copy;
        event.type = type;
        event.fromBox = from;
        event.toBox = to;
        event.actor = actor;
        event.note = note;
        event.persist();
    }
}
