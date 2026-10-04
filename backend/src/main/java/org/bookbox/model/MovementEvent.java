package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * 流转事件：实体书全生命周期轨迹。
 * 跨箱归还、破损、重新上架均留痕，可按实体编号追查完整流转。
 */
@Entity
public class MovementEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public BookCopy bookCopy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MovementType type;

    @ManyToOne(fetch = FetchType.LAZY)
    public BookBox fromBox;

    @ManyToOne(fetch = FetchType.LAZY)
    public BookBox toBox;

    /** 操作者（管理员或读者姓名） */
    public String actor;

    public String note;

    @Column(nullable = false)
    public Instant happenedAt = Instant.now();

    public static java.util.List<MovementEvent> historyOf(BookCopy copy) {
        return list("bookCopy = ?1 order by happenedAt asc, id asc", copy);
    }
}
