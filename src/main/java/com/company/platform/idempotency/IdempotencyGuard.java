package com.company.platform.idempotency;

import com.company.platform.exception.BusinessException;
import com.company.platform.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 幂等守卫（机制层）：统一「重放 / 冲突」语义。
 *
 * <p>三条语义（全公司统一）：</p>
 * <ol>
 *   <li>键不存在 → 放行，调用方执行业务后写入记录；</li>
 *   <li>键存在且指纹相同 → <b>重放</b>：放行（调用方返回首次结果，不重复执行业务）；</li>
 *   <li>键存在且指纹不同 → <b>冲突</b>：抛 {@code BusinessException(CONFLICT_001 语义码)}。</li>
 * </ol>
 */
public final class IdempotencyGuard {

    private IdempotencyGuard() {
    }

    /**
     * 计算请求指纹：对<b>规范化后的请求内容</b>（排序后的字段串 / 原始报文）取 SHA-256。
     * 内容不同 → 指纹不同；调用方负责规范化的确定性（不许把时间戳等易变字段混进去）。
     */
    public static String sha256Fingerprint(String canonicalRequest) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 校验幂等键：冲突即抛异常；重放与首次请求放行（区分由调用方按返回的记录处理）。
     *
     * @param store        幂等存储
     * @param key          幂等键
     * @param fingerprint  本次请求指纹
     * @param conflictCode 冲突错误码（各项目自己的 CONFLICT 语义码）
     * @return 键已存在的既有记录（重放场景由调用方返回首次结果）；键不存在返回空
     */
    public static java.util.Optional<IdempotencyRecord> checkOrThrow(
            IdempotencyStore store, String key, String fingerprint, ErrorCode conflictCode) {
        var existing = store.find(key);
        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint().equals(fingerprint)) {
                throw new BusinessException(conflictCode, "幂等键冲突，请求内容与首次不一致", java.util.Map.of(
                        "idempotencyKey", key,
                        "firstSeenAt", String.valueOf(existing.get().createdAt() == null
                                ? Instant.EPOCH : existing.get().createdAt())));
            }
            return existing;
        }
        return java.util.Optional.empty();
    }
}
