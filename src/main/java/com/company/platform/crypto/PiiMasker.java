package com.company.platform.crypto;

/**
 * 个人信息脱敏（机制层）：日志与响应里禁止明文个人信息，统一走这里。
 *
 * <p>两个策略：保边脱中（姓名/地址类）、保尾脱头（账号/证件号类）。
 * 脱敏结果只用于展示与日志，不可逆。</p>
 */
public final class PiiMasker {

    private PiiMasker() {
    }

    /**
     * 保边脱中：保留首尾各 keep 个字符（不足 2*keep+1 全部打码）。
     * 例：keep=1 时「山田太郎」→「山**太郎」。
     */
    public static String maskMiddle(String plain, int keep) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        int n = plain.length();
        if (n <= 2 * keep) {
            return "*".repeat(n);
        }
        return plain.substring(0, keep) + "*".repeat(n - 2 * keep) + plain.substring(n - keep);
    }

    /**
     * 保尾脱头：只保留末尾 keep 个字符（不足 keep 全部打码）。
     * 例：keep=4 时「12345678」→「****5678」。
     */
    public static String maskTail(String plain, int keep) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        int n = plain.length();
        if (n <= keep) {
            return "*".repeat(n);
        }
        return "*".repeat(n - keep) + plain.substring(n - keep);
    }
}
