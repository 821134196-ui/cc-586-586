package org.community.bookbox.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 捐赠者。匿名捐赠者的身份信息不对读者展示。 */
@Entity
@Table(name = "donors")
public class Donor extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    public String contact;

    /** 匿名捐赠：读者视图只显示“匿名捐赠” */
    @Column(nullable = false)
    public boolean anonymous = false;
}
