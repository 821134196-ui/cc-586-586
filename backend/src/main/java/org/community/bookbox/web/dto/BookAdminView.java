package org.community.bookbox.web.dto;

import org.community.bookbox.model.Book;
import org.community.bookbox.model.Loan;

import java.time.LocalDateTime;

/** 管理员视图：包含捐赠人身份与当前借阅人信息。 */
public record BookAdminView(
        String code,
        String title,
        String isbn,
        String author,
        String condition,
        String status,
        String boxCode,
        String boxName,
        LocalDateTime createdAt,
        String donorName,
        String donorContact,
        boolean donorAnonymous,
        String coverUrl,
        Long activeLoanId,
        String activeBorrower,
        LocalDateTime activeDueAt
) {
    public static BookAdminView of(Book book, Loan activeLoan) {
        return new BookAdminView(
                book.code,
                book.title,
                book.isbn,
                book.author,
                book.condition.name(),
                book.status.name(),
                book.currentBox != null ? book.currentBox.code : null,
                book.currentBox != null ? book.currentBox.name : null,
                book.createdAt,
                book.donor != null ? book.donor.name : null,
                book.donor != null ? book.donor.contact : null,
                book.donor != null && book.donor.anonymous,
                book.coverImage != null ? "/api/covers/" + book.coverImage : null,
                activeLoan != null ? activeLoan.id : null,
                activeLoan != null ? activeLoan.borrowerName : null,
                activeLoan != null ? activeLoan.dueAt : null
        );
    }
}
