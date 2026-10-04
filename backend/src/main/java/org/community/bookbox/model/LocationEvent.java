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
import java.util.List;

/** 流转事件：一本实体书从捐赠到当前位置的完整轨迹，跨箱归还也可追查。 */
@Entity
@Table(name = "location_events", indexes = {
        @Index(name = "idx_events_book", columnList = "book_id")
})
public class LocationEvent extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    public Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public LocationEventType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_box_id")
    public Box fromBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_box_id")
    public Box toBox;

    /** 操作人：管理员、读者姓名等 */
    public String actor;

    @Column(length = 1024)
    public String note;

    @Column(nullable = false)
    public LocalDateTime occurredAt = LocalDateTime.now();

    public static List<LocationEvent> historyOf(Book book) {
        return list("book = ?1 order by occurredAt asc, id asc", book);
    }
}
