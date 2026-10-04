package org.community.bookbox.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 图书漂流箱（柜位） */
@Entity
@Table(name = "boxes")
public class Box extends PanacheEntityBase {

    /** SQLite 下使用 IDENTITY，避免序列表与主事务的写锁冲突 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 箱位编号，如 BOX-A */
    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false)
    public String name;

    /** 箱位所在的物理位置描述 */
    public String location;

    /** 容量上限：在架数量达到后拒绝继续上架/归还入箱 */
    @Column(nullable = false)
    public int capacity;

    public static Box findByCode(String code) {
        return find("code", code).firstResult();
    }

    /** 当前在架数量 */
    public long currentLoad() {
        return Book.count("currentBox = ?1 and status = ?2", this, BookStatus.ON_SHELF);
    }

    public boolean hasFreeSlot() {
        return currentLoad() < capacity;
    }
}
