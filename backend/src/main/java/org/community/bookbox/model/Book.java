package org.community.bookbox.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 实体书（物理副本）。每本有独立编号 code（如 BK-000001），
 * ISBN 相同的不同副本是不同的 Book 记录，可分别借出。
 */
@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_isbn", columnList = "isbn"),
        @Index(name = "idx_books_status", columnList = "status")
})
public class Book extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 实体编号，全局唯一，贴于书上供扫码/查询 */
    @Column(nullable = false, unique = true, length = 16)
    public String code;

    @Column(nullable = false)
    public String title;

    @Column(length = 20)
    public String isbn;

    public String author;

    /** 书况（管理员检查时登记/更新） */
    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false, length = 16)
    public BookCondition condition = BookCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public BookStatus status = BookStatus.PENDING_INSPECTION;

    /** 当前所在箱位；借出或待检查时为 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_box_id")
    public Box currentBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    public Donor donor;

    /** 封面图文件名（本地存储于 covers 目录） */
    public String coverImage;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public static Book findByCode(String code) {
        return find("code", code).firstResult();
    }
}
