package org.bookbox.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

import java.time.Instant;

/** 捐赠登记：居民捐书后由管理员审核、上架 */
@Entity
public class Donation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    public BookCopy bookCopy;

    /** 捐赠者姓名；匿名时可为空。仅管理员可见，读者接口绝不返回 */
    public String donorName;

    /** 匿名捐赠：对读者与公示界面隐藏身份 */
    @Column(nullable = false)
    public boolean anonymous;

    public String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DonationStatus status = DonationStatus.PENDING;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant reviewedAt;

    public String reviewNote;
}
