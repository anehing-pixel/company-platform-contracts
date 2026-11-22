package com.company.platform.idempotency;

import java.time.Instant;

/**
 * 幂等记录（机制层）：键 + 请求指纹。
 *
 * <p>同一键两次请求：指纹相同＝重放（返回首次结果），指纹不同＝冲突（拒绝）。
 * 指纹由调用方用规范化后的请求内容计算（见 {@link IdempotencyGuard#sha256Fingerprint}）。</p>
 *
 * @param key                幂等键（请求头 Idempotency-Key 或业务唯一键）
 * @param requestFingerprint 请求内容指纹（SHA-256 十六进制）
 * @param createdAt          首次写入时间
 */
public record IdempotencyRecord(String key, String requestFingerprint, Instant createdAt) {
}
