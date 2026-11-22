package com.company.platform.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 审计操作人解析（机制层）：以服务端验签得到的令牌身份为准。
 *
 * <p>优先级：令牌身份（authSubject，不可伪造）→ 请求头 X-Operator-Id（无登录态时兜底）
 * → 字面量 {@code "system"}。与 floorplan 版同构，主体属性名对齐
 * {@link PlatformJwtAuthInterceptor}。</p>
 */
public final class AuditActor {

    public static final String ATTR_AUTH_SUBJECT = PlatformJwtAuthInterceptor.ATTR_SUBJECT;
    public static final String ATTR_AUTH_SUBJECT_TYPE = PlatformJwtAuthInterceptor.ATTR_SUBJECT_TYPE;
    public static final String ATTR_AUTH_ROLES = PlatformJwtAuthInterceptor.ATTR_ROLES;

    private static final String HEADER_OPERATOR = "X-Operator-Id";
    private static final String FALLBACK = "system";

    private AuditActor() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return FALLBACK;
        }
        Object subject = request.getAttribute(ATTR_AUTH_SUBJECT);
        if (subject != null && !subject.toString().isBlank()) {
            return subject.toString().trim();
        }
        String header = request.getHeader(HEADER_OPERATOR);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return FALLBACK;
    }

    /** 主体类型字符串；无登录态返回 {@code "SYSTEM"}（兜底主体）或 null（请求为空）。 */
    public static String resolveSubjectType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object type = request.getAttribute(ATTR_AUTH_SUBJECT_TYPE);
        return type == null ? null : String.valueOf(type);
    }

    /** 角色集合的逗号串（审计留痕用）；无则返回 null。 */
    public static String resolveRoles(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object roles = request.getAttribute(ATTR_AUTH_ROLES);
        if (roles instanceof java.util.Collection<?> collection) {
            if (collection.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object r : collection) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(r);
            }
            return sb.toString();
        }
        return null;
    }
}
