package org.community.bookbox.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 请求体集合 */
public final class Requests {

    private Requests() {
    }

    public record DonationRequest(
            @NotBlank(message = "书名不能为空") String title,
            String isbn,
            String author,
            String condition,
            String donorName,
            String donorContact,
            Boolean anonymous,
            String note
    ) {
    }

    public record ShelveRequest(
            @NotBlank(message = "箱位编号不能为空") String boxCode,
            String condition,
            String note
    ) {
    }

    public record BorrowRequest(
            @NotBlank(message = "图书编号不能为空") String bookCode,
            @NotBlank(message = "借阅人姓名不能为空") String borrowerName,
            String borrowerContact
    ) {
    }

    public record ReturnRequest(
            @NotBlank(message = "图书编号不能为空") String bookCode,
            @NotBlank(message = "归还箱位不能为空") String boxCode,
            Boolean damaged,
            String note
    ) {
    }

    public record BoxRequest(
            @NotBlank(message = "箱位编号不能为空") String code,
            @NotBlank(message = "箱位名称不能为空") String name,
            String location,
            Integer capacity
    ) {
    }
}
