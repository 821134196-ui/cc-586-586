package org.bookbox.model;

/** 实体书状态 */
public enum CopyStatus {
    /** 捐赠待审核 */
    PENDING_REVIEW,
    /** 已上架（在某箱中） */
    ON_SHELF,
    /** 已借出 */
    BORROWED,
    /** 破损待处理 */
    DAMAGED,
    /** 已下架淘汰 */
    RETIRED
}
