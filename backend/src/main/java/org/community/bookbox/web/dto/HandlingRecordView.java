package org.community.bookbox.web.dto;

import org.community.bookbox.model.HandlingRecord;

import java.time.LocalDateTime;

/** 处理记录视图（管理员） */
public record HandlingRecordView(
        long id,
        String type,
        String bookCode,
        String bookTitle,
        Long loanId,
        String note,
        String handledBy,
        LocalDateTime createdAt
) {
    public static HandlingRecordView of(HandlingRecord record) {
        return new HandlingRecordView(
                record.id,
                record.type.name(),
                record.book.code,
                record.book.title,
                record.loan != null ? record.loan.id : null,
                record.note,
                record.handledBy,
                record.createdAt
        );
    }
}
