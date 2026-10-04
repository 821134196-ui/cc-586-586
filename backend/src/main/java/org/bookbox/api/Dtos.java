package org.bookbox.api;

import org.bookbox.model.*;

import java.time.Instant;

/**
 * 视图与请求对象。
 * 注意：读者侧视图（CopyView / MovementView）绝不包含捐赠者信息，保证匿名捐赠者不外泄。
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- 视图 ----------

    public record BoxView(String code, String name, String location, int capacity, long occupied) {
        public static BoxView of(BookBox box) {
            return new BoxView(box.code, box.name, box.location, box.capacity, BookBox.occupied(box));
        }
    }

    /** 读者可见的实体书信息 */
    public record CopyView(String copyCode, String title, String author, String isbn,
                           BookCondition condition, CopyStatus status,
                           String boxCode, String boxName, String boxLocation,
                           String coverUrl) {
        public static CopyView of(BookCopy copy) {
            BookBox box = copy.currentBox;
            return new CopyView(copy.copyCode, copy.title, copy.author, copy.isbn,
                    copy.bookCondition, copy.status,
                    box == null ? null : box.code,
                    box == null ? null : box.name,
                    box == null ? null : box.location,
                    copy.coverFile == null ? null : "/api/covers/" + copy.coverFile);
        }
    }

    public record MovementView(MovementType type, String fromBox, String toBox,
                               String actor, String note, Instant happenedAt) {
        public static MovementView of(MovementEvent e) {
            return new MovementView(e.type,
                    e.fromBox == null ? null : e.fromBox.code,
                    e.toBox == null ? null : e.toBox.code,
                    e.actor, e.note, e.happenedAt);
        }
    }

    public record LoanView(long id, String copyCode, String title, String borrowerName,
                           String borrowerContact, Instant borrowedAt, Instant dueAt,
                           Instant returnedAt, String returnBoxCode, boolean overdue) {
        public static LoanView of(Loan loan) {
            return new LoanView(loan.id, loan.bookCopy.copyCode, loan.bookCopy.title,
                    loan.borrowerName, loan.borrowerContact, loan.borrowedAt, loan.dueAt,
                    loan.returnedAt,
                    loan.returnBox == null ? null : loan.returnBox.code,
                    loan.isOverdue());
        }
    }

    /** 管理员可见的捐赠信息（含捐赠者，匿名时标注） */
    public record DonationAdminView(long id, String copyCode, String title, String isbn,
                                    BookCondition condition, String donorName, boolean anonymous,
                                    String note, DonationStatus status, Instant createdAt) {
        public static DonationAdminView of(Donation d) {
            return new DonationAdminView(d.id, d.bookCopy.copyCode, d.bookCopy.title,
                    d.bookCopy.isbn, d.bookCopy.bookCondition,
                    d.anonymous ? "匿名捐赠者" : d.donorName,
                    d.anonymous, d.note, d.status, d.createdAt);
        }
    }

    public record IncidentView(long id, IncidentType type, String copyCode, String title,
                               String borrowerName, String note, Instant createdAt,
                               boolean resolved, Instant resolvedAt, String handleNote) {
        public static IncidentView of(IncidentRecord r) {
            return new IncidentView(r.id, r.type, r.bookCopy.copyCode, r.bookCopy.title,
                    r.loan == null ? null : r.loan.borrowerName,
                    r.note, r.createdAt, r.resolved, r.resolvedAt, r.handleNote);
        }
    }

    // ---------- 请求 ----------

    public record BoxRequest(String code, String name, String location, int capacity) {
    }

    public record DonationRequest(String title, String author, String isbn,
                                  BookCondition condition, String donorName,
                                  boolean anonymous, String note) {
    }

    public record ApproveRequest(String boxCode) {
    }

    public record NoteRequest(String note) {
    }

    public record BorrowRequest(String copyCode, String borrowerName, String borrowerContact) {
    }

    public record ReturnRequest(String copyCode, String boxCode, String damageNote) {
    }

    public record ReshelveRequest(String boxCode, String note) {
    }
}
