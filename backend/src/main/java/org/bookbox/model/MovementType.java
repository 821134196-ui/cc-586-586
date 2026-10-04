package org.bookbox.model;

/** 流转事件类型 */
public enum MovementType {
    /** 捐赠登记 */
    DONATION_REGISTERED,
    /** 审核通过并上架 */
    SHELVED,
    /** 借出 */
    BORROWED,
    /** 归还 */
    RETURNED,
    /** 破损标记 */
    DAMAGED,
    /** 修复后重新上架 */
    RESHELVED,
    /** 淘汰下架 */
    RETIRED
}
