package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** 处理记录：逾期、破损等需管理员跟进的事项 */
@Entity
public class IncidentRecord extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public IncidentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    public Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public BookCopy bookCopy;

    @Column(nullable = false)
    public String note;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    @Column(nullable = false)
    public boolean resolved = false;

    public Instant resolvedAt;

    public String handleNote;

    public static IncidentRecord findOpen(IncidentType type, Loan loan) {
        return find("type = ?1 and loan = ?2 and resolved = false", type, loan).firstResult();
    }
}
