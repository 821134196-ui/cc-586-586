package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** 借阅记录：一次借出对应一条记录，归还时回填 */
@Entity
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public BookCopy bookCopy;

    @Column(nullable = false)
    public String borrowerName;

    public String borrowerContact;

    @Column(nullable = false)
    public Instant borrowedAt = Instant.now();

    @Column(nullable = false)
    public Instant dueAt;

    /** 归还时间；null 表示仍在借出中 */
    public Instant returnedAt;

    /** 归还到的箱（可与原箱不同） */
    @ManyToOne(fetch = FetchType.LAZY)
    public BookBox returnBox;

    /** 归还时登记的破损说明 */
    public String damageNote;

    public boolean isActive() {
        return returnedAt == null;
    }

    public boolean isOverdue() {
        return isActive() && Instant.now().isAfter(dueAt);
    }

    public static Loan findActiveByCopy(BookCopy copy) {
        return find("bookCopy = ?1 and returnedAt is null", copy).firstResult();
    }

    public static long countActiveByCopy(BookCopy copy) {
        return count("bookCopy = ?1 and returnedAt is null", copy);
    }
}
