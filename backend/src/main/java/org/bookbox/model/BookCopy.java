package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * 实体书（一册一编号）。
 * 即使书名/ISBN 相同，每本实物都有独立 copyCode，借还、定位均以 copyCode 为准。
 */
@Entity
public class BookCopy extends BaseEntity {

    /** 实体编号，如 BK-000001，全局唯一 */
    @Column(unique = true, nullable = false, length = 32)
    public String copyCode;

    @Column(nullable = false)
    public String title;

    public String author;

    /** 同一 ISBN 可对应多本实体书 */
    @Column(length = 32)
    public String isbn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public BookCondition bookCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CopyStatus status;

    /** 当前所在箱；借出/破损待处理时为 null */
    @ManyToOne(fetch = FetchType.LAZY)
    public BookBox currentBox;

    /** 封面文件名（存于数据目录 covers/ 下） */
    public String coverFile;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public static BookCopy findByCopyCode(String copyCode) {
        return find("copyCode", copyCode).firstResult();
    }
}
