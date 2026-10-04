package org.community.bookbox.web.dto;

import org.community.bookbox.model.Book;
import org.community.bookbox.model.Loan;

import java.time.LocalDateTime;

/** 读者视图：不暴露借阅人联系方式；匿名捐赠者不显示身份。 */
public record BookView(
        String code,
        String title,
        String isbn,
        String author,
        String condition,
        String status,
        String boxCode,
        String boxName,
        String boxLocation,
        LocalDateTime dueAt,
        String donorLabel,
        String coverUrl
) {
    public static BookView of(Book book, Loan activeLoan) {
        return new BookView(
                book.code,
                book.title,
                book.isbn,
                book.author,
                book.condition.name(),
                book.status.name(),
                book.currentBox != null ? book.currentBox.code : null,
                book.currentBox != null ? book.currentBox.name : null,
                book.currentBox != null ? book.currentBox.location : null,
                activeLoan != null ? activeLoan.dueAt : null,
                donorLabel(book),
                book.coverImage != null ? "/api/covers/" + book.coverImage : null
        );
    }

    static String donorLabel(Book book) {
        if (book.donor == null) {
            return null;
        }
        return book.donor.anonymous ? "匿名捐赠" : book.donor.name;
    }
}
