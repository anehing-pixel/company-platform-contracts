package com.company.platform.audit;

import java.time.Instant;
import java.util.Map;

/**
 * 审计事件结构（机制层）。
 *
 * <p><b>append-only 约定</b>：审计表只 INSERT，不 UPDATE、不 DELETE；
 * 任何项目的审计实现必须满足这一点（数据库层面用 INSERT-only 权限的 role 强制）。</p>
 *
 * @param actor      操作人（{@code AuditActor.resolve} 的结果；系统动作为 {@code "system"}）
 * @param subjectType 操作主体类型（EMPLOYEE / CUSTOMER / SYSTEM / PARTNER）
 * @param action     动作名（动词或动词短语，如 VIEW_CUSTOMER_PII、APPROVE_APPLICATION）
 * @param targetType 被操作对象类型
 * @param targetId   被操作对象 ID
 * @param at         发生时间
 * @param details    明细（<b>禁止放个人信息明文</b>——只放 ID、哈希、掩码）
 */
public record AuditEvent(String actor, String subjectType, String action,
                         String targetType, String targetId, Instant at, Map<String, Object> details) {
}
