package org.bookbox.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * 实体基类。SQLite 下必须使用 IDENTITY 主键：
 * Panache 默认的 AUTO 会退化为序列表生成器，取号时需要挂起当前事务另起连接，
 * 在单写者的 SQLite 上会直接死锁（SQLITE_BUSY）。
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
}
