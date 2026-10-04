package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/** 漂流箱（柜位） */
@Entity
public class BookBox extends BaseEntity {

    /** 箱编号，如 BOX-A */
    @Column(unique = true, nullable = false, length = 32)
    public String code;

    @Column(nullable = false)
    public String name;

    public String location;

    /** 箱容量：在架册数达到容量后拒绝继续上架 */
    @Column(nullable = false)
    public int capacity;

    public static BookBox findByCode(String code) {
        return find("code", code).firstResult();
    }

    /** 当前在架册数 */
    public static long occupied(BookBox box) {
        return BookCopy.count("currentBox = ?1 and status = ?2", box, CopyStatus.ON_SHELF);
    }

    public boolean hasFreeSlot() {
        return occupied(this) < capacity;
    }
}
