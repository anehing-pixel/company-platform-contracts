package com.company.platform.idempotency;

import com.company.platform.exception.BusinessException;
import com.company.platform.exception.ErrorCode;
import com.company.platform.exception.StandardErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 统一「重放 / 冲突」语义的可执行证据。 */
class IdempotencyGuardTest {

    private static final ErrorCode CONFLICT = StandardErrorCode.CONFLICT_001;

    static class MemStore implements IdempotencyStore {
        final Map<String, IdempotencyRecord> data = new HashMap<>();

        @Override
        public Optional<IdempotencyRecord> find(String key) {
            return Optional.ofNullable(data.get(key));
        }

        @Override
        public void save(IdempotencyRecord record) {
            data.put(record.key(), record);
        }
    }

    @Test
    void 键不存在_放行() {
        MemStore store = new MemStore();
        String fp = IdempotencyGuard.sha256Fingerprint("{\"amount\":100}");
        assertTrue(IdempotencyGuard.checkOrThrow(store, "k1", fp, CONFLICT).isEmpty());
    }

    @Test
    void 相同指纹_重放放行并返回首次记录() {
        MemStore store = new MemStore();
        String fp = IdempotencyGuard.sha256Fingerprint("{\"amount\":100}");
        store.save(new IdempotencyRecord("k1", fp, Instant.now()));
        Optional<IdempotencyRecord> hit = IdempotencyGuard.checkOrThrow(store, "k1", fp, CONFLICT);
        assertEquals("k1", hit.orElseThrow().key());
    }

    @Test
    void 相同键不同指纹_冲突拒绝() {
        MemStore store = new MemStore();
        store.save(new IdempotencyRecord("k1",
                IdempotencyGuard.sha256Fingerprint("{\"amount\":100}"), Instant.now()));
        String differentFp = IdempotencyGuard.sha256Fingerprint("{\"amount\":200}");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> IdempotencyGuard.checkOrThrow(store, "k1", differentFp, CONFLICT));
        assertEquals("CONFLICT_001", ex.getErrorCode().getCode());
    }

    @Test
    void 指纹确定性_同内容同指纹() {
        assertEquals(
                IdempotencyGuard.sha256Fingerprint("same-content"),
                IdempotencyGuard.sha256Fingerprint("same-content"));
    }
}
