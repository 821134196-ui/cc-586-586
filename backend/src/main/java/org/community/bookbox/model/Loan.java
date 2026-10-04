package org.community.bookbox.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 借阅记录。一本实体书同一时间最多一条 ACTIVE 记录。 */
@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_status", columnList = "status")
})
public class Loan extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    public Book book;

    @Column(nullable = false)
    public String borrowerName;

    public String borrowerContact;

    @Column(nullable = false)
    public LocalDateTime borrowedAt = LocalDateTime.now();

    @Column(nullable = false)
    public LocalDateTime dueAt;

    public LocalDateTime returnedAt;

    /** 归还时放入的箱位（可与借出箱不同） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_box_id")
    public Box returnBox;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public LoanStatus status = LoanStatus.ACTIVE;

    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && LocalDateTime.now().isAfter(dueAt);
    }

    public static Loan findActiveByBook(Book book) {
        return find("book = ?1 and status = ?2", book, LoanStatus.ACTIVE).firstResult();
    }
}
