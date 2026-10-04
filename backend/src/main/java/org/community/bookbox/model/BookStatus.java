package org.community.bookbox.model;

/** 实体书状态 */
public enum BookStatus {
    PENDING_INSPECTION, // 捐赠待检查
    ON_SHELF,           // 在架（某箱位中）
    BORROWED,           // 已借出
    RETIRED             // 已下架（报废/遗失）
}
