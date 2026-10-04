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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 处理记录：逾期未还、归还破损等需要管理员跟进的事项。 */
@Entity
@Table(name = "handling_records")
public class HandlingRecord extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    public Book book;

    /** 关联的借阅记录（逾期提醒/破损归还时填写） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    public Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public HandlingType type;

    @Column(length = 1024)
    public String note;

    public String handledBy;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public static boolean existsForLoanAndType(Loan loan, HandlingType type) {
        return count("loan = ?1 and type = ?2", loan, type) > 0;
    }
}
