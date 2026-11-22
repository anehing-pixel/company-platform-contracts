package com.company.platform.security;

/**
 * 主体类型（机制层）：token 属于哪一类主体。
 *
 * <p><b>切断点 ③ 的参数化落点</b>：员工 RBAC 与客户身份是两套独立体系，
 * 主体类型必须显式进 token 并在网关/拦截器层校验——客户 token 不得访问员工端点，反之亦然。
 * 平台层只提供枚举与校验机制，不定义任何角色/权限内容。</p>
 */
public enum SubjectType {
    /** 员工（后台操作者）。 */
    EMPLOYEE,
    /** C 端客户。 */
    CUSTOMER,
    /** 系统间调用（服务对服务、定时任务）。 */
    SYSTEM,
    /** 外部合作方（DMZ 签名通道对应的主体）。 */
    PARTNER
}
