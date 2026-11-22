package com.company.platform.security;

/**
 * 令牌验签端口（机制层）：把「怎么验」留给各项目，机制只消费结果。
 *
 * <p>floorplan 的验签实现在其 {@code AuthApplicationService}（员工 JWT）；
 * RF 是双主体两套密钥（{@code RETAIL_JWT_SECRET} / {@code RETAIL_CUSTOMER_JWT_SECRET}），
 * 由两个 TokenVerifier 实现分别装配两个拦截器实例。验签失败抛
 * {@code BusinessException(未授权码)}，由拦截器转 401 响应。</p>
 */
public interface TokenVerifier {

    /**
     * 验签并解析令牌。
     *
     * @param bearerToken 去掉 {@code Bearer } 前缀后的令牌文本
     * @return 验签结果；失败抛 {@code BusinessException}（HTTP 401 语义）
     */
    VerifiedToken verify(String bearerToken);
}
