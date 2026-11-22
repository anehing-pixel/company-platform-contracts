package com.company.platform.security;

import java.util.Map;
import java.util.Set;

/**
 * 验签结果（机制层）：由 {@link TokenVerifier} 实现方产出。
 *
 * <p>权限与角色一律用<b>字符串</b>——各项目的权限点枚举内容属于语义层，
 * 平台机制只负责「声明了什么、持有者有没有」，翻译工作由实现方的 verify 完成。</p>
 *
 * @param subject      主体标识（员工工号 / 客户 ID / 服务名）
 * @param subjectType  主体类型，见 {@link SubjectType}
 * @param roles        角色集合（可为空）
 * @param permissions  权限点集合（字符串，可为空）
 * @param claims       其余声明（审计、扩展用）
 */
public record VerifiedToken(String subject,
                            SubjectType subjectType,
                            Set<String> roles,
                            Set<String> permissions,
                            Map<String, Object> claims) {

    public VerifiedToken {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
