package com.company.platform.idempotency;

import java.util.Optional;

/**
 * 幂等存储端口（机制层）：实现方决定放哪里（数据库唯一表 / Redis / 内存）。
 *
 * <p>存储实现必须保证 {@code key} 唯一（数据库唯一约束），并发插入时只能成功一个。</p>
 */
public interface IdempotencyStore {

    /** 按键查记录；不存在返回空。 */
    Optional<IdempotencyRecord> find(String key);

    /** 保存记录；键已存在时由实现方保证原子性（INSERT ... ON CONFLICT 或等价机制）。 */
    void save(IdempotencyRecord record);
}
