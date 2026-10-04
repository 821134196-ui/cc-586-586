package org.community.bookbox.model;

/** 流转事件类型：完整记录一本实体书的轨迹 */
public enum LocationEventType {
    DONATED,  // 捐赠登记
    SHELVED,  // 检查通过并上架到某箱
    BORROWED, // 被借出（离开箱位）
    RETURNED, // 归还（放入某箱，可能与借出箱不同）
    RETIRED   // 下架
}
