package org.community.bookbox.web.dto;

import org.community.bookbox.model.Loan;
import org.community.bookbox.model.LoanStatus;

import java.time.LocalDateTime;

/** 借阅记录视图（管理员） */
public record LoanView(
        long id,
        String bookCode,
        String bookTitle,
        String borrowerName,
        String borrowerContact,
        LocalDateTime borrowedAt,
        LocalDateTime dueAt,
        LocalDateTime returnedAt,
        String returnBoxCode,
        String status,
        boolean overdue
) {
    public static LoanView of(Loan loan) {
        return new LoanView(
                loan.id,
                loan.book.code,
                loan.book.title,
                loan.borrowerName,
                loan.borrowerContact,
                loan.borrowedAt,
                loan.dueAt,
                loan.returnedAt,
                loan.returnBox != null ? loan.returnBox.code : null,
                loan.status.name(),
                loan.status == LoanStatus.ACTIVE && LocalDateTime.now().isAfter(loan.dueAt)
        );
    }
}
